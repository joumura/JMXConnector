package jp.co.tokyo_gas.cirius_fw.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * JMX経由で指定した情報を取得し、標準出力に出力する.
 */
public class Main {

	/**
	 * main.
	 * 
	 * @param args コマンドライン引数
	 */
	public static void main(String[] args) {
		try {
			Main main = new Main();
			main.output(args[0]);
		} catch (Exception ex) {
			System.err.println(ex);
		}
	}

	static final File propFile = new File("JMXConnector.propertites");
	
	Main() throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(propFile))) {
			List<String> queriesList = new ArrayList<String>();
			
			String str;
			while ((str = br.readLine()) != null) {
				if (str.length() != 0 && '#' != str.charAt(0)) {
					queriesList.add(str);
				}
			}
			queries = queriesList.toArray(new String[0]);
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}

	String[] queries;

	void output(String targetStr)
			throws IOException, AgentLoadException, AgentInitializationException, 
			MalformedObjectNameException, AttributeNotFoundException,
			InstanceNotFoundException, MBeanException, ReflectionException {
		List<VirtualMachineDescriptor> vms = VirtualMachine.list();
		for (VirtualMachineDescriptor desc : vms) {
			JMXConnector connector = null;
			try {
				connector = getJMXConnector(desc, targetStr);
				if (null == connector) {
					continue;
				}
				MBeanServerConnection connection = connector.getMBeanServerConnection();

//				Set<ObjectName> resultSet = connection.queryNames(null, null);
//				for (ObjectName objName : resultSet) {
//					System.out.println("MBean name: " + objName);
//				}
				
//				ObjectName queryName = new ObjectName("WebSphere:type=JvmStats");
//				resultSet = connection.queryNames(queryName, null);
//				for (ObjectName objName : resultSet) {
//					System.out.println("WebSphere: " + objName);
//					Hashtable<String, String> keys = objName.getKeyPropertyList();
//				    for(Entry<String, String> e : keys.entrySet()) {
//				        System.out.println("   " + e.getKey() + " : " + e.getValue());
//				    }
//				}

//				Object attr = connection.getAttribute(new ObjectName("WebSphere:type=JvmStats"), "FreeMemory");
//		        System.out.println("   FreeMemory: " + attr);

				List<String> resultList = new ArrayList<String>();
				resultList.add(desc.id());
				for (String q : queries) {
					try {
						String[] qs = q.split(" ");
						Object attr = connection.getAttribute(new ObjectName(qs[0]), qs[1]);
						resultList.add(String.valueOf(attr));
//						System.out.println(desc.id() + "\t" + qs[0] + "\t" + qs[1] + "\t" + attr);
					} catch (Exception ex) {
						resultList.add("-");
						System.err.println(desc.id() + "\t" + ex);
					}
				}
				String[] results = resultList.toArray(new String[0]);
				System.out.println("\"" + String.join("\",\"", results) + "\"");
				
			} finally {
				if (null != connector) {
					connector.close();
				}
			}
		}
	}

	static final String PID = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    static final String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	
	JMXConnector getJMXConnector(VirtualMachineDescriptor desc, String targetStr)
			throws IOException, AgentLoadException, AgentInitializationException {
	    VirtualMachine vm;
    	if (desc.displayName().contains(targetStr)
    		&& !PID.equals(desc.id())) {
		    try {
		    	vm = VirtualMachine.attach(desc);
		    } catch (AttachNotSupportedException e) {
		    	System.err.println("Attach Not Supported. (" + desc.displayName() + ")");
		        return null;
		    }
    	} else {
	        return null;
    	}
	    String connectorAddress = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
	    // load JMX agent manually
	    if (connectorAddress == null) {
            String agent = vm.getSystemProperties().getProperty("java.home")
            	+ File.separator + "lib" + File.separator + "management-agent.jar";
            vm.loadAgent(agent);
		    connectorAddress = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
	    }
	    // for IBM Java
	    // http://stackoverflow.com/questions/9081752/attaching-to-the-j9vm-using-the-attach-api
	    if (connectorAddress == null) {
	        String agent = "instrument,"
	                + vm.getSystemProperties().getProperty("java.home")
	                + File.separator + "lib" + File.separator
	                + "management-agent.jar=";
	        vm.loadAgentLibrary(agent);

	        connectorAddress = vm.getSystemProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
	    }
	    if (connectorAddress == null) {
	    	System.err.println("connectorAddress not found. (" + desc.displayName() + ")");
	        return null;
	    }
	    JMXServiceURL url = new JMXServiceURL(connectorAddress);
	    Map<String, Object> env = new HashMap<>();
	    env.put("jmx.remote.x.request.waiting.timeout", new Long(10000));
	    return JMXConnectorFactory.connect(url, env);
	}

}
