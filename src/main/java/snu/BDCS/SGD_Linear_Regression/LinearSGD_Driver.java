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

import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.evaluator.AllocatedEvaluator;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.time.event.StartTime;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Driver code for the REEF Application
 * Road map : 1) LinearSGD_Driver -> 2) StartHandler -> 3) EvaluatorAllocatorHandler
 */
@Unit
public final class LinearSGD_Driver {

  private static final Logger LOG = Logger.getLogger(LinearSGD_Driver.class.getName());

  private final EvaluatorRequestor requestor;

  /**
   * Job driver constructor - instantiated via TANG.
   * @param requestor evaluator requestor object used to create new evaluator containers.
   */
  @Inject
  public LinearSGD_Driver(final EvaluatorRequestor requestor) {
    LOG.log(Level.FINE, "Instantiated 'LinearSGD_Driver'");

    // TODO Set EvaluatorRequestor of this class
    this.requestor = requestor;
  }

  /**
   * Handles the StartTime event: Request as single Evaluator.
   */
  public final class StartHandler implements EventHandler<StartTime> {
    @Override
    public void onNext(final StartTime startTime) {
      LOG.log(Level.INFO, "Requested Evaluator.");

      // TODO Submit request for an evaluator. Build an EvaluatorRequest.
      LinearSGD_Driver.this.requestor.submit(
    		  EvaluatorRequest.newBuilder()
    		  .setMemory(128)
    		  .setNumber(3)
    		  .build());
    }
  }

  /**
   * Handles AllocatedEvaluator: Build and Context & Task Configuration
   * and submit them to the Driver
   */
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
}