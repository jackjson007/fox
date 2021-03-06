package com.fox.rpc.remoting.invoker.proxy;

import com.fox.rpc.SpiServiceLoader;
import com.fox.rpc.common.bean.InvokeRequest;
import com.fox.rpc.common.bean.InvokeResponse;
import com.fox.rpc.common.util.StringUtil;
import com.fox.rpc.registry.RemotingServiceDiscovery;
import com.fox.rpc.remoting.invoker.api.CallFuture;
import com.fox.rpc.remoting.invoker.api.Client;
import com.fox.rpc.remoting.invoker.api.ClientFactory;
import com.fox.rpc.remoting.invoker.config.ConnectInfo;
import com.fox.rpc.remoting.invoker.config.InvokerCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by shenwenbo on 16/8/23.
 */
public class ServiceInvocationProxy<T> implements InvocationHandler{


    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInvocationProxy.class);

    private String serviceAddress;

    private RemotingServiceDiscovery serviceDiscovery;

    private String serviceVersion = "";

    private Class<T> interfaceClass;

    private String serviceName;

    public ServiceInvocationProxy(InvokerCfg<T> cfg) {
        this.serviceDiscovery=cfg.getServiceDiscovery();
        this.interfaceClass=cfg.getServiceInterface();
        this.serviceName=cfg.getServiceName();

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        InvokeRequest request=createInvokeRequest(method,args);
        if (serviceDiscovery != null) {
//            if (StringUtil.isNotEmpty(serviceVersion)) {
//                serviceName += "-" + serviceVersion;
//            }
            serviceAddress = serviceDiscovery.discover(serviceName);
            LOGGER.debug("discover service: {} => {}", serviceName, serviceAddress);
        }
        if (StringUtil.isEmpty(serviceAddress)) {
            throw new RuntimeException("server address is empty");
        }
        // 从 RPC 服务地址中解析主机名与端口号
        String[] array = StringUtil.split(serviceAddress, ":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);

        //启动客户端并创建连接
        ClientFactory clientFactory= SpiServiceLoader.getExtension(ClientFactory.class);
        clientFactory.init();

        // 创建 RPC 客户端对象并发送 RPC 请求
        Client client = clientFactory.getClient(new ConnectInfo(host,port));
        client.send(request);
        Robot r   =   new   Robot();


        r.delay(3000);
        //client.setContext(host,port);
        long time = System.currentTimeMillis();
        //CallFuture callFuture=client.send(request);
//        InvokeResponse response = client.send(request);
        InvokeResponse response=client.getResponse();
        System.out.println();
        LOGGER.debug("time: {}ms", System.currentTimeMillis() - time);
        if (response == null) {
            throw new RuntimeException("response is null");
        }
        // 返回 RPC 响应结果
        if (response.hasException()) {
            throw response.getException();
        } else {
            return response.getResult();
        }
    }

    /**
     * 封装rpc调用请求
     * @param method
     * @param args
     * @return
     */
    private InvokeRequest createInvokeRequest(Method method, Object[] args) {
        InvokeRequest request = new InvokeRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setServiceVersion(serviceVersion);
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setServiceName(this.serviceName);
        return request;
    }

}
