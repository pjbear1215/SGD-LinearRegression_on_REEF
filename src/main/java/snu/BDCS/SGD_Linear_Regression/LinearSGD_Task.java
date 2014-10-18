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

import com.microsoft.reef.task.Task;

import javax.inject.Inject;

/**
 * A 'REEF' Task.
 */
public final class LinearSGD_Task implements Task {
  @Inject
  LinearSGD_Task() {
  }

  @Override
  public final byte[] call(final byte[] memento) {
    // TODO print a message.
    return null;
  }
}