/**
 * Copyright 2011 Green Energy Corp.
 * 
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.gnu.org/licenses/agpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.frontend;

import net.agileautomata.executor4s.ExecutorService;
import net.agileautomata.executor4s.Executors;
import net.agileautomata.executor4s.Minutes;
import org.totalgrid.reef.app.ConnectionCloseManagerEx;
import org.totalgrid.reef.app.impl.ApplicationManagerSettings;
import org.totalgrid.reef.app.impl.SimpleConnectedApplicationManager;
import org.totalgrid.reef.broker.BrokerConnectionFactory;
import org.totalgrid.reef.broker.qpid.QpidBrokerConnectionFactory;
import org.totalgrid.reef.client.settings.AmqpSettings;
import org.totalgrid.reef.client.settings.NodeSettings;
import org.totalgrid.reef.client.settings.UserSettings;
import org.totalgrid.reef.protocol.api.Protocol;
import org.totalgrid.reef.protocol.api.ProtocolManager;
import org.totalgrid.reef.util.ShutdownHook;

import java.util.Properties;

/**
 * When integrating a new protocol it can be very valuable to have an entry point that can be run
 * directly from an IDE. This allows a much faster development cycle versus rebuilding the protocol
 * bundle and loading it into a running reef instance.
 *
 * You will need to have a server running and load all of the properties from
 * org.totalgrid.reef.amqp.cfg, org.totalgrid.reef.user.cfg and org.totalgrid.reef.node.cfg
 * files. You will also want to have a model loaded one or more endpoints configured to use the correct
 * protocolName.
 */
public class StandaloneProtocolAdapter
{

    private ExecutorService exe;

    private ConnectionCloseManagerEx manager;

    private SimpleConnectedApplicationManager applicationManager;

    /**
     * @param properties all of the Properties from o.t.r.amqp.cfg, o.t.r.user.cfg and o.t.r.node.cfg files
     * @param protocolName name of the protocol to report to the services
     * @param protocolManager protocolManager implementation
     */
    public StandaloneProtocolAdapter( Properties properties, String protocolName, ProtocolManager protocolManager )
    {
        UserSettings userSettings = prepareApplicationManager( properties );
        applicationManager.addConnectedApplication( new FepConnectedApplication( protocolName, protocolManager, userSettings ) );
    }

    /**
     * @param properties all of the Properties from o.t.r.amqp.cfg, o.t.r.user.cfg and o.t.r.node.cfg files
     * @param protocol scala specific protocol implementation
     */
    public StandaloneProtocolAdapter( Properties properties, Protocol protocol )
    {
        UserSettings userSettings = prepareApplicationManager( properties );
        applicationManager.addConnectedApplication( new FepConnectedApplication( protocol, userSettings ) );
    }

    /**
     * runs the protocol until a shutdown signal is encountered
     */
    public void run()
    {
        manager.start();
        applicationManager.start();
        ShutdownHook.waitForShutdown( new Runnable() {
            public void run()
            {
                applicationManager.stop();
                manager.stop();
                exe.terminate();
            }
        } );
    }

    private UserSettings prepareApplicationManager( Properties properties )
    {
        UserSettings userSettings = new UserSettings( properties );
        AmqpSettings brokerSettings = new AmqpSettings( properties );
        NodeSettings nodeSettings = new NodeSettings( properties );
        BrokerConnectionFactory brokerConnection = new QpidBrokerConnectionFactory( brokerSettings );
        exe = Executors.newResizingThreadPool( new Minutes( 5 ) );

        manager = new ConnectionCloseManagerEx( brokerConnection, exe );

        ApplicationManagerSettings appManagerSettings = new ApplicationManagerSettings( userSettings, nodeSettings );
        applicationManager = new SimpleConnectedApplicationManager( exe, manager, appManagerSettings );
        return userSettings;
    }
}