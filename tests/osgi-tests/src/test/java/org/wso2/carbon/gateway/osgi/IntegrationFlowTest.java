/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.gateway.osgi;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ExamFactory;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.container.CarbonContainerFactory;
import org.wso2.carbon.container.options.CarbonDistributionOption;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.Constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Stack;
import javax.inject.Inject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyDropinsBundle;
import static org.wso2.carbon.container.options.CarbonDistributionOption.copyFile;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@ExamFactory(CarbonContainerFactory.class)
/**
 * Test class to test simple integration flow.
 */
public class IntegrationFlowTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationFlowTest.class);

    @Inject
    private BundleContext bundleContext;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private CarbonMessageProcessor carbonMessageProcessor;

    @Configuration
    public Option[] createConfiguration() {
        return new Option[]{
                copyPassthroughSampleOption(),
                CarbonDistributionOption.keepDirectory(),
                copyDropinsBundle(maven().artifactId("mockito-all").groupId("org.mockito")
                        .versionAsInProject())
                //CarbonDistributionOption.debug()
        };
    }

    @Test
    public void testIntegrationFlow()throws Exception {
        /*Integration sampleIntegration = IntegrationConfigRegistry.getInstance().getIntegrationConfig("simpleflow");
        MediatorCollection generatedMediatorCollection = sampleIntegration.getResource("passthrough").getDefaultWorker()
                .getMediators();
        List<Mediator> mediatorList = generatedMediatorCollection.getMediators();*/

        CarbonMessage carbonMessage = mock(CarbonMessage.class);
        CarbonCallback carbonCallback = mock(CarbonCallback.class);

        when(carbonMessage.getProperty(Constants.LISTENER_INTERFACE_ID)).thenReturn("default");
        when(carbonMessage.getProperty(Constants.TO)).thenReturn("/stocks/getStocks");
        when(carbonMessage.getProperty("HTTP_METHOD")).thenReturn("GET");

        when(carbonMessage.getProperty(org.wso2.carbon.gateway.core.Constants.SERVICE_CONTEXT)).thenReturn("/stocks");
        when(carbonMessage.getProperty(org.wso2.carbon.gateway.core.Constants.SERVICE_SUB_GROUP_PATH)).
                thenReturn("/getStocks");
        when(carbonMessage.getProperty(org.wso2.carbon.gateway.core.Constants.SERVICE_METHOD)).
                thenReturn("GET");
        when(carbonMessage.getProperty(org.wso2.carbon.gateway.core.Constants.VARIABLE_STACK)).
                thenReturn(new Stack<Map<String, Object>>());

        /*doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((CarbonCallback) invocation.getArguments()[0]).done(carbonMessage);
                return null;
            }
        }).when(carbonCallback).done(carbonMessage);*/

        carbonMessageProcessor.receive(carbonMessage, carbonCallback);

        verify(carbonCallback, timeout(1000)).done(carbonMessage);
    }

    /**
     * Deploy the simple passThrough sample
     */
    private Option copyPassthroughSampleOption() {
        Path passthroughSamplePath;

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        passthroughSamplePath = Paths.get(basedir, "src", "test", "resources", "artifacts", "simpleflow.ballerina");
        return copyFile(passthroughSamplePath, Paths.get("deployment", "integration-flows", "simpleflow.ballerina"));
    }
}
