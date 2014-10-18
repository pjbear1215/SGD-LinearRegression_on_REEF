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

import com.microsoft.reef.driver.TaskSubmittable;
import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.evaluator.AllocatedEvaluator;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.impl.BlockingEventHandler;
import com.microsoft.wake.time.event.StartTime;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Driver code for the REEF Application
 * Road map : 1) LinearSGD_Driver -> 2) StartHandler -> 3) EvaluatorAllocatorHandler
 */
@Unit
public final class LinearSGD_Driver {

    private static final Logger LOG = Logger.getLogger(LinearSGD_Driver.class.getName());

    private final int computeTasks;

    private static final int controllerTasks = 1;

    private final AtomicInteger compTasksRunning = new AtomicInteger(0);

    private final TaskSubmitter taskSubmitter;

    private final BlockingEventHandler<ActiveContext> contextAccumulator;

    private final EvaluatorRequestor requestor;

    public static class Parameters {
        @NamedParameter(default_value = "3", doc = "The number of compute tasks to spawn")
        public static class ComputeTasks implements Name<Integer> {
        }

        @NamedParameter(default_value = "5678", doc = "Port on which Name Service should listen")
        public static class NameServicePort implements Name<Integer> {
        }
    }

    /**
     * Job driver constructor - instantiated via TANG.
     *
     * @param requestor evaluator requestor object used to create new evaluator containers.
     */
    @Inject
    public LinearSGD_Driver(
            final EvaluatorRequestor requestor,
            final @Parameter(Parameters.ComputeTasks.class) int computeTasks,
            final @Parameter(Parameters.NameServicePort.class) int nameServicePort) {

        LOG.log(Level.INFO, "Instantiated 'LinearSGD_Driver'");
        System.out.print("Instantiated 'LinearSGD_Driver'\n");
        this.requestor = requestor;
        this.computeTasks = computeTasks;
        this.taskSubmitter = new TaskSubmitter(this.computeTasks, nameServicePort);
        this.contextAccumulator = new BlockingEventHandler<ActiveContext>(
                this.computeTasks + this.controllerTasks, this.taskSubmitter);
    }

    final class AllocatedEvaluatorHandler implements EventHandler<AllocatedEvaluator> {
        @Override
        public final void onNext(final AllocatedEvaluator eval) {
            LOG.log(Level.INFO, "Received an AllocatedEvaluator. Submitting it.");
            System.out.print("Received an AllocatedEvaluator. Submitting it.\n");
            try {
                eval.submitContext(ContextConfiguration.CONF.set(ContextConfiguration.IDENTIFIER, "SGD_Linear").build());
            } catch (final BindException e) {
                throw new RuntimeException(e);
            }
        }
    }

    final class RunningTaskHandler implements EventHandler<RunningTask> {
        @Override
        public final void onNext(final RunningTask task) {
            LOG.log(Level.INFO, "Task \"{0}\" is running!", task.getId());
            System.out.print("Task " + task.getId() + " is running!\n");
            if (compTasksRunning.incrementAndGet() == computeTasks) {
                // All compute tasks are running - launch controller task
                taskSubmitter.submitControlTask();
            }
        }
    }

    final class CompletedTaskHandler implements EventHandler<CompletedTask> {
        @Override
        @SuppressWarnings("ConvertToTryWithResources")
        public final void onNext(final CompletedTask completed) {
            LOG.log(Level.INFO, "Task {0} is done.", completed.getId());
            System.out.print("Task " + completed.getId() + " is done.\n");
            if (taskSubmitter.controllerCompleted(completed.getId())) {
                // Get results from controller
                System.out.println("****************** RESULT ******************");
                System.out.println(new String(completed.get()));
                System.out.println("********************************************");
            }
            final ActiveContext context = completed.getActiveContext();
            LOG.log(Level.INFO, "Releasing Context {0}.", context.getId());
            System.out.print("Releasing Context " + context.getId() + ".\n");
            context.close();
        }
    }

    final class ActiveContextHandler implements EventHandler<ActiveContext> {
        @Override
        public void onNext(final ActiveContext activeContext) {
            LOG.log(Level.INFO, "Received a RunningEvaluator with ID: {0}", activeContext.getId());
            System.out.print("Received a RunningEvaluator with ID: " + activeContext.getId() + ".\n");
            contextAccumulator.onNext(activeContext);
        }
    }

    /**
     * Handles the StartTime event: Request as single Evaluator.
     */
    public final class StartHandler implements EventHandler<StartTime> {
        @Override
        public void onNext(final StartTime startTime) {
            LOG.log(Level.INFO, "StartTime: {0}", startTime);
            System.out.print("StartTime: " + startTime + "\n");
            LinearSGD_Driver.this.requestor.submit(
                    EvaluatorRequest.newBuilder()
                            .setNumber(computeTasks + controllerTasks)
                            .setMemory(128)
                            .setNumberOfCores(4)
                            .build());
        }

        @Override
        public String toString() {
            return "HelloDriver.StartHandler";
        }
    }

    /**
     * Handles AllocatedEvaluator: Build and Context & Task Configuration
     * and submit them to the Driver
     */
    /*
    public final class EvaluatorAllocatedHandler implements EventHandler<AllocatedEvaluator> {
        @Override
        public void onNext(final AllocatedEvaluator allocatedEvaluator) {
            LOG.log(Level.INFO, "Submitting LinearSGD_REEF task to AllocatedEvaluator: {0}", allocatedEvaluator);
            try {
                // TODO Build ContextConfiguration
                final Configuration contextConfiguration = ContextConfiguration.CONF
                        .set(ContextConfiguration.IDENTIFIER, "HelloContext").build();

                // TODO Build TaskConfiguration
                final Configuration taskConfiguration = TaskConfiguration.CONF
                        .set(TaskConfiguration.IDENTIFIER, "LinearSGD_Task")
                        .set(TaskConfiguration.TASK, LinearSGD_Task.class).build();

                // Let's submit context and task to the evaluator
                allocatedEvaluator.submitContextAndTask(contextConfiguration, taskConfiguration);
            } catch (final BindException ex) {
                throw new RuntimeException("Unable to setup Task or Context configuration.", ex);
            }
        }
    }
    */

}