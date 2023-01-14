/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.health.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.Startup;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.health.MicroProfileHealthReporter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CDIExtension implements Extension {

    private final String MP_HEALTH_DISABLE_DEFAULT_PROCEDURES = "mp.health.disable-default-procedures";


    private final MicroProfileHealthReporter reporter;
    private final Module module;
    private final Supplier<BeanManager> beanMangerSupplier;

    // Use a single Jakarta Contexts and Dependency Injection instance to select and destroy all HealthCheck probes instances
    private Instance<Object> instance;
    private final List<HealthCheck> livenessChecks = new ArrayList<>();
    private final List<HealthCheck> readinessChecks = new ArrayList<>();
    private final List<HealthCheck> startupChecks = new ArrayList<>();
    private HealthCheck defaultReadinessCheck;
    private HealthCheck defaultStartupCheck;


    public CDIExtension(MicroProfileHealthReporter healthReporter, Module module, Supplier<BeanManager> beanMangerSupplier) {
        this.reporter = healthReporter;
        this.module = module;
        this.beanMangerSupplier = beanMangerSupplier;
    }

    /**
     * Get Jakarta Contexts and Dependency Injection <em>instances</em> of HealthCheck and
     * add them to the {@link MicroProfileHealthReporter}.
     */
    private void afterDeploymentValidation(@Observes final AfterDeploymentValidation avd) {
        instance = beanMangerSupplier.get().createInstance();

        addHealthChecks(Liveness.Literal.INSTANCE, reporter::addLivenessCheck, livenessChecks);
        addHealthChecks(Readiness.Literal.INSTANCE, reporter::addReadinessCheck, readinessChecks);
        addHealthChecks(Startup.Literal.INSTANCE, reporter::addStartupCheck, startupChecks);
        reporter.setUserChecksProcessed(true);
        if (readinessChecks.isEmpty()) {
            Config config = ConfigProvider.getConfig(module.getClassLoader());
            boolean disableDefaultprocedure = config.getOptionalValue(MP_HEALTH_DISABLE_DEFAULT_PROCEDURES, Boolean.class).orElse(false);
            if (!disableDefaultprocedure) {
                // no readiness probe are present in the deployment. register a readiness check so that the deployment is considered ready
                defaultReadinessCheck = new DefaultReadinessHealthCheck(module.getName());
                reporter.addReadinessCheck(defaultReadinessCheck, module.getClassLoader());
            }
        }
        if (startupChecks.isEmpty()) {
            Config config = ConfigProvider.getConfig(module.getClassLoader());
            boolean disableDefaultprocedure = config.getOptionalValue(MP_HEALTH_DISABLE_DEFAULT_PROCEDURES, Boolean.class).orElse(false);
            if (!disableDefaultprocedure) {
                // no startup probes are present in the deployment. register a startup check so that the deployment is considered started
                defaultStartupCheck = new DefaultStartupHealthCheck(module.getName());
                reporter.addStartupCheck(defaultStartupCheck, module.getClassLoader());
            }
        }
    }

    private void addHealthChecks(AnnotationLiteral qualifier,
                                 BiConsumer<HealthCheck, ClassLoader> healthFunction, List<HealthCheck> healthChecks) {
        for (HealthCheck healthCheck : instance.select(HealthCheck.class, qualifier)) {
            healthFunction.accept(healthCheck, module.getClassLoader());
            healthChecks.add(healthCheck);
        }
    }

    /**
     * Called when the deployment is undeployed.
     * <p>
     * Remove all the instances of {@link HealthCheck} from the {@link MicroProfileHealthReporter}.
     */
    public void beforeShutdown(@Observes final BeforeShutdown bs) {
        removeHealthCheck(livenessChecks, reporter::removeLivenessCheck);
        removeHealthCheck(readinessChecks, reporter::removeReadinessCheck);
        removeHealthCheck(startupChecks, reporter::removeStartupCheck);

        if (defaultReadinessCheck != null) {
            reporter.removeReadinessCheck(defaultReadinessCheck);
            defaultReadinessCheck = null;
        }

        if (defaultStartupCheck != null) {
            reporter.removeStartupCheck(defaultStartupCheck);
            defaultStartupCheck = null;
        }

        instance = null;
    }

    private void removeHealthCheck(List<HealthCheck> healthChecks,
                                   Consumer<HealthCheck> healthFunction) {
        for (HealthCheck healthCheck : healthChecks) {
            healthFunction.accept(healthCheck);
            instance.destroy(healthCheck);
        }
        healthChecks.clear();
    }

    public void vetoSmallryeHealthReporter(@Observes ProcessAnnotatedType<SmallRyeHealthReporter> pat) {
        pat.veto();
    }

    private static final class DefaultReadinessHealthCheck implements HealthCheck {

        private final String deploymentName;

        DefaultReadinessHealthCheck(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("ready-" + deploymentName)
                    .up()
                    .build();
        }
    }

    private static final class DefaultStartupHealthCheck implements HealthCheck {

        private final String deploymentName;

        DefaultStartupHealthCheck(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named("started-" + deploymentName)
                .up()
                .build();
        }
    }
}
