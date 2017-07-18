/*
 * Copyright (c) 2015 Tata Consultancy Services and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sdninterfaceapp.impl;

//基本上每个文件都会引入controller类的一些函数
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.topology.msg.rev151006.OpendaylightSdniTopologyMsgService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//首先每一个功能都会有一个***Provider.java文件保存在impl/src/main/java/org/opendaylight/hello/impl/文件夹下面，可以添加
//任何要实现的功能进来，譬如这里，是调用SdniTopologyMsgServiceImpl类生成一个对象具体功能在其中实现
//第二步是在api/src/main/yang/文件夹下面创建一个****.yang文件用于定义其RPC的输入输出参数类型
//第三步是定义****Service.java文件，与Provider同目录，实现具体的函数功能--》SdniTopologyMsgServiceImpl.java

public class SdniTopologyProvider implements BindingAwareProvider, AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SdniTopologyProvider.class);
    private DataBroker dataBroker = null;
    private RpcRegistration<OpendaylightSdniTopologyMsgService> sdniTopologyServiceRpc; 

    @Override
    public void onSessionInitiated(ProviderContext session) {
    	LOG.info("SdniTopology Plugin Started");
        SdniTopologyMsgServiceImpl sdniTopologyMsgServiceImpl = SdniTopologyMsgServiceImpl.getInstance();
        sdniTopologyServiceRpc = session.addRpcImplementation(OpendaylightSdniTopologyMsgService.class,  sdniTopologyMsgServiceImpl);
        dataBroker = session.getSALService(DataBroker.class);   
        sdniTopologyMsgServiceImpl.setBroker(dataBroker);
    }

    @Override
    public void close() throws Exception {
        LOG.info("SdniTopologyProvider Closed");
        if (sdniTopologyServiceRpc != null) {
            sdniTopologyServiceRpc.close();
        }
    }
}
