package org.paul.core;

import lombok.extern.slf4j.Slf4j;
import org.paul.common.utils.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class ConfigLoader {
    private static final String CONFIG_FILE = "gateway.properties";
    private static final String ENV_PREFIX = "GATEWAY_";
    private static final String JVM_PREFIX = "gateway.";

    // 实现单例设计模式
    private static final ConfigLoader INSTANCE = new ConfigLoader();
    private ConfigLoader() {}

    public static ConfigLoader getInstance() {
        return INSTANCE;
    }

    // 封装config对象，用于存放配置
    private Config config;

    // 提供静态方法，获取配置
    public static Config getConfig() {
        return INSTANCE.config;
    }

    // 核心加载流程
    // load方法，加载配置，有一定优先级，高优先级会覆盖低优先级
    // 优先级：运行参数14444 -> 启动时jvm参数19999 -> 环境变量9499 -> 配置文件9899 -> config中的默认值 9888
    public Config load(String[] args) {
        // 由低到高实现
        // new相当于使用默认值
        config = new Config();

        // 配置文件
        loadFromConfigFile();

        // 环境变量
        loadFromEnv();

        // 启动时jvm参数
        loadFromJvm();

        // 运行参数
        loadFromArgs(args);
        return config;
    }

    /**
     * 启动时，运行参数读取配置
     * @param args
     */
    private void loadFromArgs(String[] args){
        // 例子：指定端口号 --port = 1234
        // 如果args不为空
        if(args != null & args.length > 0){
            Properties properties = new Properties();
            // 循环args里面的参数
            for(String arg: args){
                // 判断以 -- 开头  并且  包含等号
                if(arg.startsWith("--") && arg.contains("=")){
                    // 将值放入properties中，需要对数据进行截断
                    properties.put(arg.substring(2, arg.indexOf("=")),
                            arg.substring(arg.indexOf("=") + 1));
                }
            }
            // 将properties赋值到config
            PropertiesUtils.properties2Object(properties, config);
        }

    }

    /**
     * 启动时jvm参数读取配置
     */
    private void  loadFromJvm(){
        Properties properties = System.getProperties();
        PropertiesUtils.properties2Object(properties, config, JVM_PREFIX);
    }

    /**
     * 从环境变量加载
     */
    private void loadFromEnv(){
        // Map,用于读取环境变量
        Map<String, String> env = System.getenv();
        // new Properties，用于读取数据
        Properties properties = new Properties();
        // 将环境变量放入Properties
        properties.putAll(env);
        // 拷贝到config
        PropertiesUtils.properties2Object(properties, config, ENV_PREFIX);
    }

    /**
     * 从配置文件加载
     * @return
     */
    private void loadFromConfigFile(){
        // 从gateway.properties文件加载配置，通过ConfigLoader的类加载器读取文件
        InputStream resourceAsStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        // 如果能拿到，即输入流不为空
        if(resourceAsStream != null){
            // 封装到Properties对象中
            Properties properties = new Properties();
            // 将属性值复制到config对象中
            try{
                properties.load(resourceAsStream);
                PropertiesUtils.properties2Object(properties, config);
            }
            // 异常处理
            catch(IOException e){
                log.warn("load config file {} error:", CONFIG_FILE, e);
            }
            // 最后关闭io
            finally {
                // 关闭io
                if(resourceAsStream != null){
                    try{
                        resourceAsStream.close();
                    }
                    catch (IOException e){
                        //
                    }
                }
            }
        }




    }
}