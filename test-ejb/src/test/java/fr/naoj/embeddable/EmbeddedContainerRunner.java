package fr.naoj.embeddable;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.eclipse.jst.j2ee.commonarchivecore.internal.CommonarchiveFactory;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EJBJarFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.exception.OpenFailureException;
import org.eclipse.jst.j2ee.commonarchivecore.internal.strategy.LoadStrategy;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EjbFactory;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.metadata.ClassDataObject;
import com.ibm.ws.metadata.ClassDataObjectFields;
import com.ibm.ws.metadata.MetaDataSources;
import com.ibm.ws.metadata.ModuleDataObject;
import com.ibm.ws.metadata.WCCMConfigReader;
import com.ibm.ws.metadata.annotations.AnnotationConfigReader;
import com.ibm.ws.runtime.component.MetaDataMgrImpl;
import com.ibm.ws.util.ImplFactory;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;

import fr.naoj.embeddable.annotation.DataSetDefinition;
import fr.naoj.embeddable.annotation.Module;
import fr.naoj.embeddable.annotation.Schema;
import fr.naoj.embeddable.archive.Archive;

/**
 * Runner that is responsible to initialize the embedded container. Then, it scans the test class
 * to inject any bean that must be used, managed by the embedded container.
 * 
 * @author Johann Bernez
 */
public class EmbeddedContainerRunner extends BlockJUnit4ClassRunner {
	
	private List<Field> fields = new ArrayList<Field>();
	private EJBContainer ec;
	private List<EJBModule> modules;
	private IDataSet testDataSet;
	private IDataSet dataSet;
	private IDatabaseConnection dbConnection;
	
	private static final Logger LOG = Logger.getLogger(EmbeddedContainerRunner.class.getName());
	
	public EmbeddedContainerRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		
		// Search in the test class all the field that must be injected
		Class<?> currentClass = clazz;
		Method paramsMethod = null;
		String bindingName = null;
		while (currentClass != null && !currentClass.equals(Object.class)) {
			for (Field f : currentClass.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(EJB.class)) {
					fields.add(f);
				}
			}
			for (Method m : currentClass.getDeclaredMethods()) {
				m.setAccessible(true);
				if (Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(Module.class)) {
					bindingName = m.getAnnotation(Module.class).bindingName();
					paramsMethod = m;
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		if (bindingName != null && paramsMethod != null) {
			Archive archive;
			try {
				archive = (Archive) paramsMethod.invoke(null, new Object[0]);
			} catch (Exception e) {
				throw new InitializationError(e);
			}
			
			initializeContainer(bindingName, archive);
			
			initializeDBUnit(bindingName);
		} else {
			throw new InitializationError("No static method annotated with @" + Module.class.getSimpleName() + " found in class : " + clazz);
		}
	}
	
	@Override
	public void run(RunNotifier notifier) {
		super.run(notifier);
		if (ec != null) {
			try {
				if (testDataSet != null) {
					DatabaseOperation.DELETE.execute(dbConnection, testDataSet);
				}
			} catch (Exception e) {
				fail("Unable to remove the dataset");
			}
			ec.close();
		}
	}
	
	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		boolean delete = false;
		if (method.getAnnotation(DataSetDefinition.class) != null) {
			DataSetDefinition def = (DataSetDefinition) method.getAnnotation(DataSetDefinition.class);
			delete = def.delete();
			
			// Initialize the dataset
			try {
				dataSet = createDataSet(def.value());
				DatabaseOperation.INSERT.execute(dbConnection, dataSet);
			} catch (Exception e) {
				notifier.fireTestFailure(new Failure(Description.createTestDescription(method.getMethod().getClass(), method.getMethod().getName()), e));
				notifier.pleaseStop();
			}
		}
		super.runChild(method, notifier);
		
		if (delete) {
			try {
				DatabaseOperation.DELETE.execute(dbConnection, dataSet);
			} catch (Exception e) {
				notifier.fireTestFailure(new Failure(Description.createTestDescription(method.getMethod().getClass(), method.getMethod().getName()), e));
			}
		}
	}
	
	@Override
	protected Object createTest() throws Exception {
		Object test = super.createTest();
		for (Field f : this.fields) {
			try {
				f.set(test, lookup(f.getType()));
			} catch (Exception e) {
				throw new InitializationError(e);
			}
		}
		
		return test;
	}
	
	protected void initializeContainer(String bindingName, Archive archive) throws InitializationError {
		try {
			// Map that will contains all the properties necessary 
			// for the embedded container
			Map<String, Object> properties = new HashMap<String, Object>();
			
			File archiveFile = archive.asFile();
			createEjbModules(archiveFile);
			
			properties.put("com.ibm.websphere.embeddable.configFileName", "src/test/resources/embeddable.properties");
			properties.put(EJBContainer.MODULES, new File[]{archiveFile});
			
			String wholeBindingName;
			for (EJBModule module : this.modules) {
				wholeBindingName = module.getResourceBindingRef() + bindingName;
				LOG.info("Binding [" + wholeBindingName + "] to [" + bindingName + "]");
				properties.put(wholeBindingName, bindingName);
			}
			
			ec = EJBContainer.createEJBContainer(properties);
		} catch (Exception e) {
			throw new InitializationError(Arrays.asList(new Throwable[]{e}));
		}
	}
	
	protected void initializeDBUnit(String bindingName) throws InitializationError {
		try {
			// Fetching the datasource, this is necessary for DBUnit unit which
			// needs a reference to the connection
			DataSource ds = (DataSource) new InitialContext().lookup(bindingName);
			
			String schemaName = "";
			Schema schema = getTestClass().getAnnotation(Schema.class);
			if (schema != null) {
				schemaName = schema.value();
			}
			
			if (!schemaName.isEmpty()) {
				dbConnection = new DatabaseConnection(ds.getConnection(), schemaName);
		    	dbConnection.getConfig().setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
		    	
		    	DataSetDefinition def = getTestClass().getAnnotation(DataSetDefinition.class);
		    	if (def != null) {
		    		testDataSet = createDataSet(def.value());
		    		DatabaseOperation.INSERT.execute(dbConnection, testDataSet);
		    	}
			} else {
				LOG.info("DBUnit not used, schema not defined");
			}
		} catch (Exception e) {
			throw new InitializationError(Arrays.asList(new Throwable[]{e}));
		}
	}
	
	/**
	 * <p>Dinamically lookup for a given bean. When the lookup has to made
	 * manually, here is the type of jdni to use :<br/>
	 * <code>lookup("java:global/MODULE_NAME/BEAN_NAME");</code><br/>
	 * for exemple :<br/>
	 * <code>lookup("java:global/test-ejb-0.0.1-SNAPSHOT/TestService");</code></p>
	 * 
	 * @param clazz
	 * @return
	 * @throws NamingException
	 */
	@SuppressWarnings("unchecked")
	private <T> T lookup(Class<T> clazz) throws NamingException {
		for (EJBModule module : this.modules) {
			LOG.info("Lookup for module " + module.moduleName);
			if (module.ejbClass.isAssignableFrom(clazz) || clazz.isAssignableFrom(module.ejbClass)) {
				return (T) ec.getContext().lookup("java:global/" + module.moduleName + "/" + module.ejbClass.getSimpleName());
			}
		}
		return null;
	}
	
	/**
	 * For a given file of the classpath, checks whether or not
	 * it contains some EJB in it, and so return a list of them.
	 * 
	 * @param file
	 * @return a non null list of potential EJB
	 */
	private void createEjbModules(File file) {
		String logicalModuleName;
		LoadStrategy loadStrategy;
		EJBJarFile ejbJarFile;
		
		if (hasEjbJar(file)) {
			try {
				ejbJarFile = CommonarchiveFactory.eINSTANCE.openEJBJarFile(file.getPath());
			} catch (OpenFailureException ex) {
				throw new EJBException(ex);
			}
			logicalModuleName = ejbJarFile.getDeploymentDescriptor().getModuleName();
		} else {
			String uri = file.getPath();
			try {
				loadStrategy = CommonarchiveFactory.eINSTANCE.createLoadStrategy(uri);
			} catch (IOException ex) {
				throw new EJBException(ex);
			}
			EJBJar ejbJar = EjbFactory.eINSTANCE.createEJBJar();
			ejbJar.setVersion("3.0");
			ejbJarFile = CommonarchiveFactory.eINSTANCE.createEJBJarFile();
			ejbJarFile.setURI(uri);
			ejbJarFile.setLoadStrategy(loadStrategy);
			ejbJarFile.setDeploymentDescriptor(ejbJar);
			logicalModuleName = null;
		}
		
		String moduleName = file.getName();
		if (logicalModuleName == null) {
			logicalModuleName = ComponentNameSpaceConfiguration.getLogicalModuleName(moduleName);
		}

		MetaDataSources mds = new MetaDataSources();
		mds.iv_Sources[MetaDataSources.sv_ZipFileIndex] = null;
		try {
			MetaDataMgrImpl factory = new MetaDataMgrImpl();
			factory.initialize(null);
			mds.iv_Sources[MetaDataSources.sv_ModuleNameIndex] = moduleName;
			mds.iv_Sources[MetaDataSources.sv_LogicalModuleNameIndex] = logicalModuleName;
			mds.iv_Sources[MetaDataSources.sv_MetaDataManagerIndex] = factory;
			mds.iv_Sources[MetaDataSources.sv_ModuleRefIndex] = CommonarchiveFactory.eINSTANCE.createEJBModuleRef(ejbJarFile);
			mds.iv_Sources[MetaDataSources.sv_ApplicationNameIndex] = "embeddable";
			mds.iv_Sources[MetaDataSources.sv_ClassLoaderIndex] = getTestClass().getJavaClass().getClassLoader();
			mds.iv_Sources[MetaDataSources.sv_FilesIndex] = Collections.singletonList(file);
			
			ModuleDataObject mdo = new ModuleDataObject(((J2EENameFactory) ImplFactory.loadImplFromKey(J2EENameFactory.class)).create("embeddable", moduleName, null));
			new WCCMConfigReader().populateModuleData(mdo, mds);
			new AnnotationConfigReader(true).populateModuleData(mdo, mds);
			
			String className;
			modules = new ArrayList<EJBModule>();
			for (ClassDataObject cdo : mdo.getAllClassDataObjects()) {
				className = (String) cdo.getEntry(ClassDataObjectFields.CLASS_NAME);
				Class<?> c = Class.forName(className);
				if (c.isAnnotationPresent(Resource.class)) {
					modules.add(new EJBModule(logicalModuleName, c));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to create the ejb modules", e);
		}
	}
	
	/**
	 * Checks in the given file if it contains an ejb-jar.xml file.
	 * 
	 * @param file
	 * @return <code>true</code> if the ejb-jar.xml is found, <code>false</code> otherwise
	 */
	private boolean hasEjbJar(File file) {
		boolean hasEJBJarXML;
		ZipFile zipFile;
		if (file.isDirectory()) {
			zipFile = null;
			hasEJBJarXML = new File(file, "META-INF/ejb-jar.xml").exists();
		} else {
			try {
				zipFile = new ZipFile(file);
			} catch (IOException ex) {
				throw new EJBException(ex);
			}
			hasEJBJarXML = zipFile.getEntry("META-INF/ejb-jar.xml") != null;
		}
		return hasEJBJarXML;
	}
	
	/**
	 * Create from an xml resource in the classpath the corresponding
	 * {@link IDataSet} if possible and if the resource is correct
	 * (respects the DBunit dataset definition).
	 * 
	 * @param dataSetName the name of the xml resource
	 * @return the {@link IDataSet} reflecting the dataSetName
	 * @throws DataSetException if the dataSetName can not be found in the classpath
	 */
	private IDataSet createDataSet(String dataSetName) throws DataSetException {
		return new FlatXmlDataSetBuilder().build(getClass().getClassLoader().getResourceAsStream(dataSetName));
	}
	
	/**
	 * Class holding basic information about en ejb :
	 * <ul>
	 * <li>its module name</li>
	 * <li>its class</li>
	 * </ul>
	 * 
	 * @author Johann Bernez
	 */
	private static class EJBModule {
		private final String moduleName;
		private final Class<?> ejbClass;
		
		public EJBModule(String moduleName, Class<?> ejbClass) {
			this.moduleName = moduleName;
			this.ejbClass = ejbClass;
		}
		
		public String getResourceBindingRef() {
			return "Bean.#"+this.moduleName+"#"+this.ejbClass.getSimpleName()+".ResourceRef.BindingName.";
		}
		
		@Override
		public String toString() {
			return "Module ["+moduleName+"], class ["+ejbClass+"]";
		}
	}
}
