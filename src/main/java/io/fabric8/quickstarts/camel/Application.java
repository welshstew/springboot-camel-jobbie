/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.quickstarts.camel;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends RouteBuilder {

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configure() throws Exception {
        from("timer://foo?period=60000")
            .setBody().constant("Hello World")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        OpenShiftClient osClient = new DefaultOpenShiftClient();

                        int randomNumber = ThreadLocalRandom.current().nextInt();

                        String jobName = "job-" + randomNumber;

                        Job aJob = new JobBuilder()
                                .withNewMetadata().withName(jobName).addToLabels("job-name", jobName).endMetadata()
                                .withNewSpec()
                                .withNewTemplate()
                                .withNewMetadata().addToLabels("job-name", jobName).endMetadata()
                                .withNewSpec()
                                .withRestartPolicy("Never")
                                .addNewContainer().withName(jobName).withImage("registry.access.redhat.com/rhel7/rhel:latest")
                                .withCommand("/bin/bash", "-c", "for i in {1..5}; do echo hi stuff; sleep 5; done")
                                .withNewResources()
                                .addToRequests("cpu", new Quantity("100m"))
                                .addToRequests("memory", new Quantity("128Mi"))
                                .addToLimits("cpu", new Quantity("100m"))
                                .addToLimits("memory", new Quantity("128Mi"))
                                .endResources()
                                .endContainer()
                                .endSpec()
                                .endTemplate()
                                .endSpec().build();

                        osClient.batch().jobs().inNamespace("springboot").create(aJob);

                    }
                })
            .log(">>> ${body}");
    }
}
