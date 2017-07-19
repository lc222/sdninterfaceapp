/*
 * Copyright (c) 2015 Tata Consultancy Services and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sdninterfaceapp.impl;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.sdninterfaceapp.impl.database.SdniDataBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.GetAllPeerNodeConnectorsStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.OpendaylightSdniQosMsgService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.SdnControllers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.SdnControllersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.get.all.peer.node.connectors.statistics.output.Controllers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.get.all.peer.node.connectors.statistics.output.ControllersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.NodeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.NodeListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.NodeListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.node.list.PortList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.node.list.PortListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.node.list.port.list.PortParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.nodes.node.list.port.list.PortParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpendaylightSdniQosMsgServiceImpl implements OpendaylightSdniQosMsgService {

	private static OpendaylightSdniQosMsgServiceImpl serviceObj = null;
	private final Logger logger = LoggerFactory.getLogger(OpendaylightSdniQosMsgServiceImpl.class);//用来记录日志 目前还不清楚是否有输出的功能
	private List<String> sdnControllers = new ArrayList<String>(); //用来记录ip地址
	private InstanceIdentifier<SdnControllers> instanceIdentifier = InstanceIdentifier.builder(SdnControllers.class).build(); //注意此处 instanceIdentifier 是 SdnControllers.class 后面有写操作
	private DataBroker dataBroker;//主要作用就是使用其中的方法来从datastore中获取需要的数据

	private OpendaylightSdniQosMsgServiceImpl(){
		//getAllNodeConnectorsStatistics();
	}

	public static OpendaylightSdniQosMsgServiceImpl getInstance()//没有则实例化类的对象，有则获取对象，确定该类仅存在一个静态的对象
	{
		if ( serviceObj == null )
		{
			serviceObj = new OpendaylightSdniQosMsgServiceImpl();
		}
		return serviceObj;
	}


	@Override
	public Future<RpcResult<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
	sdninterfaceapp.qos.msg.rev151006.GetAllNodeConnectorsStatisticsOutput>> 
	getAllNodeConnectorsStatistics() {//获取本机控制器控制下的网络的所有节点的信息
		logger.info("SdniQoSReader - getNodeConnectorStatistics :  Start");
		String controller = null;

		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006.GetAllNodeConnectorsStatisticsOutput output = null;
		RpcResultBuilder rpcBuilder = null;//最后返回的东西

		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sdninterfaceapp.qos.msg.rev151006
		.GetAllNodeConnectorsStatisticsOutputBuilder builder = null;
		NodeList nodeList = null;
		List<NodeList> outputNodesList = new ArrayList<NodeList>(); //NodeList是节点对象，存放的是节点的信息
		try {

			controller = findIpAddress(); //本机ip地址
			final ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction(); //异步读对象

			InstanceIdentifier<Nodes> NODES_IDENTIFIER = InstanceIdentifier.create(Nodes.class);
			Nodes nodes = getDataObject(readTx, NODES_IDENTIFIER);//注意此处,nodes得到的是所有的节点，与上一步联系起来看就是，初始化一个读取事件，并读取datastore中有关nodes的数据
			if(nodes!=null) {
				List<Node> nodesList = nodes.getNode();
				if(nodesList!=null&& !nodesList.isEmpty()) {
					for ( Node node : nodesList )
					{
						try {
							nodeList = getAllPortStats(node, readTx);//得到当前节点的所有的端口信息
						} catch (ReadFailedException | ExecutionException
								| InterruptedException e) {
							logger.error("Exception in getAllNodeConnectorsStatistics : "+e.getMessage());
						}

						outputNodesList.add(nodeList);//将得到的nodeList加入到输出中
					}

				}
			}
			else
			{
				logger.error("in getAllNodeConnectorsStatistics : nodes : " + nodes);
			}

		} catch (Exception e) {
			logger.error("Exception in getAllNodeConnectorsStatistics : "+e.getMessage());
			rpcBuilder = RpcResultBuilder.failed();
		}
		builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
				.yang.sdninterfaceapp.qos.msg.rev151006.GetAllNodeConnectorsStatisticsOutputBuilder();
		builder.setNodeList(outputNodesList);
		builder.setControllerIp(controller);

		if ( !sdnControllers.contains(controller) ) {
			sdnControllers.add(controller);//如果当前sdnControllers中没有包含controller ip则添加进去
		}

		SdnControllersBuilder scb = new SdnControllersBuilder();
		scb.setSdnController(sdnControllers);




		final WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();



		writeTx.merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, scb.build()); // instanceIdentifier 是 SdnControllers.class 此处是向datastore进行写操作
		 writeTx.commit();//提交
		
		output = builder.build();//builder包含所有上面经过处理后的数据

		rpcBuilder = RpcResultBuilder.success(output);//rpcBuilder目前还不知道功能
		return rpcBuilder.buildFuture();
	}



	private <T extends DataObject> T getDataObject(final ReadTransaction readOnlyTransaction,
			final InstanceIdentifier<T> identifier) { //从数据库中得到对应类型的数据
		Optional<T> optionalData = null;
		try {
			optionalData = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, identifier).get();// 读取identifier对应的数据，LogicalDatastoreType.OPERATIONAL好像是默认参数，也存在与writer中
			if (optionalData.isPresent()) {
				return optionalData.get();
			}
		} catch (Exception e) {
			logger.error("Exception while executing getDataObject: " + e.getMessage());
		}
		return null;
	}


	private NodeList getAllPortStats(Node node, ReadOnlyTransaction readTx) 
			throws ExecutionException, InterruptedException, ReadFailedException {

		org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics
		.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics flow = null; 

		final NodeKey nodeKey = node.getKey();
		logger.info("In getAllPortStats nodekey : " + node.getId().getValue() + " : " + node.getKey());


		List<NodeConnector> ncList = node.getNodeConnector(); //

		if ( ncList == null || ncList.isEmpty() )
		{
			return null;
		}

		
		logger.info("In getAllPortStats ncList size : "  + node.getId().getValue() + " : " +  ncList.size());

		List<PortList> portList = new ArrayList<PortList>();
		for (NodeConnector nc : ncList )
		{
		      final InstanceIdentifier<FlowCapableNodeConnector> connectorRef = InstanceIdentifier
		    	        .create(Nodes.class).child(Node.class, nodeKey)
		    	        .child(NodeConnector.class, nc.getKey())
		    	        .augmentation(FlowCapableNodeConnector.class);//数据所在位置
		      FlowCapableNodeConnector nodeConnector = getDataObject(readTx, connectorRef);//得到对应的数据
		      logger.info("In getAllPortStats nodeConnector.getName() :{} ", nodeConnector.getName());
		      final InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
		    		  .child(Node.class, nodeKey).child(NodeConnector.class, nc.getKey());
			final Optional<FlowCapableNodeConnectorStatisticsData> flowCapableNodeConnectorStatisticsDataOptional =
					readTx.read(LogicalDatastoreType.OPERATIONAL,
							nodeConnectorII.augmentation(FlowCapableNodeConnectorStatisticsData.class)).checkedGet();

			if(flowCapableNodeConnectorStatisticsDataOptional.isPresent())
			{
				flow = flowCapableNodeConnectorStatisticsDataOptional.get().getFlowCapableNodeConnectorStatistics();
				//下面的内容参见NetworkCapabilitiesQOS部分的代码

				List<PortParams> portparams = new ArrayList<PortParams>();
				PortListBuilder portListBuilder = new PortListBuilder();
				portListBuilder.setPortId(nc.getId().getValue());
				PortParamsBuilder portparamsBuilder = new PortParamsBuilder();
				portparamsBuilder.setBytes(flow.getBytes());
				portparamsBuilder.setCollisionCount(flow.getCollisionCount());
				portparamsBuilder.setDuration(flow.getDuration());
				portparamsBuilder.setPackets(flow.getPackets());
				portparamsBuilder.setReceiveCrcError(flow.getReceiveCrcError());
				portparamsBuilder.setReceiveDrops(flow.getReceiveDrops());
				portparamsBuilder.setReceiveErrors(flow.getReceiveErrors());
				portparamsBuilder.setReceiveFrameError(flow.getReceiveFrameError());
				portparamsBuilder.setReceiveOverRunError(flow.getReceiveOverRunError());
				portparamsBuilder.setTransmitDrops(flow.getTransmitDrops());
				portparamsBuilder.setTransmitErrors(flow.getTransmitErrors());
				portparamsBuilder.setPortName(nodeConnector.getName());
				portparams.add(portparamsBuilder.build());
				portListBuilder.setPortParams(portparams);
				portList.add(portListBuilder.build());

				//TO-DO
/*				PortStatistics ps = new PortStatistics();
                ps.setController(controller);
                ps.setNodeID(nc.getId().getValue());
                ps.setPortID(nc.getId().getValue());
                ps.setPortName( nodeConnector.getName());
                ps.setReceiveCrcError(portparamsBuilder.getReceiveCrcError().toString());
                ps.setReceiveFrameError(portparamsBuilder.getReceiveFrameError().toString());
                ps.setReceiveOverRunError(portparamsBuilder.getReceiveOverRunError().toString());
                ps.setCollisionCount(portparamsBuilder.getCollisionCount().toString());
                ps.setTransmitPackets(portparamsBuilder.getPackets().getTransmitted().toString());
                ps.setReceivePackets(portparamsBuilder.getPackets().getReceived().toString());
				
				list_QoS.add(ps);*/
			}
		}

		NodeListBuilder nodeListBuilder = new NodeListBuilder();
		nodeListBuilder.setKey(new NodeListKey(node.getId().getValue()));
		nodeListBuilder.setNodeId(node.getId().getValue());
		nodeListBuilder.setPortList(portList);

		return nodeListBuilder.build();
	}

	private String findIpAddress() {// get this machine's ip
		Enumeration e = null;
		try {
			e = NetworkInterface.getNetworkInterfaces();//Returns all the interfaces on this machine.
		} catch (SocketException e1) {
			logger.error("Failed to get list of interfaces", e1);
			return null;
		}
		while (e.hasMoreElements()) {

			NetworkInterface n = (NetworkInterface) e.nextElement();

			Enumeration ee = n.getInetAddresses();//Convenience method to return an Enumeration with all or a subset of the InetAddresses bound to this network interface.
			while (ee.hasMoreElements()) {
				InetAddress i = (InetAddress) ee.nextElement();
				logger.debug("Trying address {}", i);
				if ((i instanceof Inet4Address) && (!i.isLoopbackAddress())) {
					String hostAddress = i.getHostAddress();
					logger.debug("Settled on controller address {}", hostAddress);
					return hostAddress;
				}
			}
		}
		logger.error("Failed to find a suitable controller address");
		return null;
	}


	@Override
	public Future<RpcResult<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
	sdninterfaceapp.qos.msg.rev151006.GetAllPeerNodeConnectorsStatisticsOutput>> getAllPeerNodeConnectorsStatistics() {
		logger.info("In getAllPeerNodeConnectorsStatistics START");
		GetAllPeerNodeConnectorsStatisticsOutputBuilder outputBuilder = new GetAllPeerNodeConnectorsStatisticsOutputBuilder();
		List<Controllers> controllers = new ArrayList<Controllers>();

		try {
			SdniDataBase sdb = SdniDataBase.getInstance();//数据库

			Map<String,Map<String,Map<String, PortStatistics>>> qosData = sdb.getAllQoSPeerData();// controllers:
			if ( qosData != null && !qosData.isEmpty() )
			{
				//get controller info
				Set<String> controllersList = qosData.keySet();//controllersList 是peer controller的集合
				if ( controllersList != null && !controllersList.isEmpty() )
				{
					logger.info("In getAllPeerNodeConnectorsStatistics controllersList : {}", controllersList.size());

					for ( String controllerIp : controllersList )
					{
						ControllersBuilder crtlBuilder = new ControllersBuilder();
						//Get node info
						List<NodeList> nodeList = new ArrayList<NodeList>();
						Map<String,Map<String, PortStatistics>> nodeData = qosData.get(controllerIp);//对应控制器下的节点的集合

						if ( nodeData != null && !nodeData.isEmpty() )
						{
							logger.info("In getAllPeerNodeConnectorsStatistics nodeData : {}", nodeData.size());

							Set<String> nodeNames = nodeData.keySet();//节点名
							if ( nodeNames != null && !nodeNames.isEmpty() )
							{

								for ( String nodeName : nodeNames )
								{
									NodeListBuilder nLBuilder = new NodeListBuilder();

									//Get nodeconnectors(ports) info
									List<PortList> ports = new ArrayList<PortList>();
									Map<String, PortStatistics> ncList = nodeData.get(nodeName);//节点对应的nc 网卡的字典
									if ( ncList != null && !ncList.isEmpty() )
									{
										logger.info("In getAllPeerNodeConnectorsStatistics nodeConnectorList : {}", ncList.size());
										Set<String> portsList = ncList.keySet();//端口集合
										if ( portsList != null && !portsList.isEmpty() )
										{
											for ( String port : portsList )
											{
												PortStatistics portStatistics = ncList.get(port);//portStatistics存储着端口的所有信息

												PortListBuilder portListBuilder = new PortListBuilder();

												List<PortParams> portparams = new ArrayList<PortParams>();
												PortParamsBuilder portparamsBuilder = new PortParamsBuilder();

												//下面的步骤是将portStatistics的信息放入到portparamsBuilder中去

												portparamsBuilder.setCollisionCount(new BigInteger(portStatistics.getCollisionCount()));
												portparamsBuilder.setReceiveCrcError(new BigInteger(portStatistics.getReceiveCrcError()));

												portparamsBuilder.setReceiveDrops(new BigInteger(portStatistics.getReceivePackets()));

												portparamsBuilder.setReceiveFrameError(new BigInteger(portStatistics.getReceiveFrameError()));
												portparamsBuilder.setReceiveOverRunError(new BigInteger(portStatistics.getReceiveOverRunError()));
												portparamsBuilder.setTransmitDrops(new BigInteger(portStatistics.getTransmitPackets()));

												portparams.add(portparamsBuilder.build());
												portListBuilder.setPortId(port);
												portListBuilder.setPortParams(portparams);

												ports.add(portListBuilder.build());
											}
										}
									}

									nLBuilder.setNodeId(nodeName);
									nLBuilder.setPortList(ports);
									nodeList.add(nLBuilder.build());
								}
							}
						}
						crtlBuilder.setControllerIp(controllerIp);
						crtlBuilder.setNodeList(nodeList);

						controllers.add(crtlBuilder.build());//逐次添加信息，层层嵌套


						if ( !sdnControllers.contains(controllerIp) ) {
							sdnControllers.add(controllerIp);//若sdnControllers中不存在此ip的controller则添加进去
						}
						
					}
				}
				//若要修改Qos信息，只需要关注上面的内容即可

			}
		} catch (Exception e) {
			logger.error("Exception in getAllPeerNodeConnectorsStatistics : {}",e.getMessage());
			//return RpcResultBuilder.failed().buildFuture();
		}

		outputBuilder.setControllers(controllers);
		//下面部分的内容同函数getAllNodeConnectorsStatistics()



		SdnControllersBuilder scb = new SdnControllersBuilder();
		scb.setSdnController(sdnControllers);




		final WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();



		writeTx.merge(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, scb.build());
		 writeTx.commit();
		return RpcResultBuilder.success(outputBuilder.build()).buildFuture();
	}

	public void setDataBroker(final DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}
}
