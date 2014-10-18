/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package snu.BDCS.SGD_Linear_Regression;

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.io.data.loading.api.DataLoadingRequestBuilder;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.runtime.yarn.client.YarnClientConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.CommandLine;
import org.apache.hadoop.mapred.TextInputFormat;
import sun.security.krb5.Config;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Client for Hello REEF example.
 */
public final class LinearSGD_REEF {

    private static final Logger LOG = Logger.getLogger(LinearSGD_REEF.class.getName());

    private static final int NUM_LOCAL_THREADS = 4;
    private static final int TIMEOUT = 1 * 60 * 1000;
    private static final int NUM_COMPUTE_EVALUATORS = 4;
    private static final int NUM_SPLITS = 4;

    /**
     * Command line parameter = true to run locally, or false to run on YARN.
     */
    @NamedParameter(doc = "Whether or not to run on the local runtime",
            short_name = "local", default_value = "true")
    public static final class Local implements Name<Boolean> {
    }

    @NamedParameter(doc = "Number of minutes before timeout",
            short_name = "timeout", default_value = "1")
    public static final class TimeOut implements Name<Integer> {
    }

    @NamedParameter(doc = "Input Path", short_name = "input", default_value = "yacht_hydrodynamics_data.csv")
    public static final class InputDir implements Name<String> {
    }

    /**
     * @return the configuration of the LinearSGD_REEF driver.
     */

    public static LauncherStatus run(final Configuration runtimeConf, final int timeOut)
            throws BindException, InjectionException {
        final Configuration driverConfiguration = DriverConfiguration.CONF
                .set(DriverConfiguration.DRIVER_IDENTIFIER, "SGD_Linear")
                .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(LinearSGD_Driver.class))
                .set(DriverConfiguration.ON_DRIVER_STARTED, LinearSGD_Driver.StartHandler.class)
                .set(DriverConfiguration.ON_EVALUATOR_ALLOCATED, LinearSGD_Driver.AllocatedEvaluatorHandler.class)
                .set(DriverConfiguration.ON_TASK_RUNNING, LinearSGD_Driver.RunningTaskHandler.class)
                .set(DriverConfiguration.ON_TASK_COMPLETED, LinearSGD_Driver.CompletedTaskHandler.class)
                .set(DriverConfiguration.ON_CONTEXT_ACTIVE, LinearSGD_Driver.ActiveContextHandler.class)
                .build();
        // DriverLauncher launches Driver to run the application.
        return DriverLauncher.getLauncher(runtimeConf).run(driverConfiguration, timeOut);
    }

    /**
     * Start REEF job. Runs method run().
     *
     * @param args command line parameters.
     * @throws com.microsoft.tang.exceptions.BindException      configuration error.
     * @throws com.microsoft.tang.exceptions.InjectionException configuration error.
     */
    public static void main(final String[] args) throws BindException, InjectionException {
        // TODO : build LocalRuntimeConfiguration

        /*
        final Tang tang = Tang.Factory.getTang();

        final JavaConfigurationBuilder cb = tang.newConfigurationBuilder();

        try {
            new CommandLine(cb)
                    .registerShortNameOfClass(Local.class)
                    .registerShortNameOfClass(TimeOut.class)
                    .registerShortNameOfClass(LinearSGD_REEF.InputDir.class)
                    .processCommandLine(args);

        } catch (final IOException ex) {
            LOG.log(Level.SEVERE, "Configuration error: cannot load data", ex);
            throw new RuntimeException("Configuration error: cannot load data", ex);
        }

        final Injector injector = tang.newInjector(cb.build());

        final boolean isLocal = injector.getNamedInstance(Local.class);
        final int jobTimeout = injector.getNamedInstance(TimeOut.class);
        final String inputDir = injector.getNamedInstance(LinearSGD_REEF.InputDir.class);

        final Configuration runtimeConfiguration;
        if (isLocal) {
            LOG.log(Level.INFO, "Running on the local runtime");
            runtimeConfiguration = LocalRuntimeConfiguration.CONF
                .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, NUM_LOCAL_THREADS).build();
        } else {
            LOG.log(Level.INFO, "Running on YARN");
            runtimeConfiguration = YarnClientConfiguration.CONF.build();
        }

        final EvaluatorRequest computeRequest = EvaluatorRequest.newBuilder()
                .setNumber(NUM_COMPUTE_EVALUATORS)
                .setMemory(1024)
                .setNumberOfCores(4)
                .build();

        final Configuration dataLoadConfiguration = new DataLoadingRequestBuilder()
                .setMemoryMB(1024)
                .setInputFormatClass(TextInputFormat.class)
                .setInputPath(inputDir)
                .setNumberOfDesiredSplits(NUM_SPLITS)
                .setComputeRequest(computeRequest)
                .setDriverConfigurationModule(DriverConfiguration.CONF
                        .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(LinearSGD_Driver.class))
                        .set(DriverConfiguration.ON_CONTEXT_ACTIVE, LinearSGD_Driver.ContextActiveHandler.class)
                        .set(DriverConfiguration.ON_TASK_COMPLETED, LinearSGD_Driver.TaskCompletedHandler.class)
                        .set(DriverConfiguration.DRIVER_IDENTIFIER, "DataLoadingREEF"))
                .build();

        final LauncherStatus status = run(runtimeConfiguration, JOB_TIMEOUT);
        */
        final Configuration runtimeConfiguration = LocalRuntimeConfiguration.CONF
                .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, NUM_LOCAL_THREADS)
                .build();
        final LauncherStatus status = run(runtimeConfiguration, TIMEOUT);
        LOG.log(Level.INFO, "REEF job completed: {0}", status);
    }
}