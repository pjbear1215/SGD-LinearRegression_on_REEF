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

#include "Clr2JavaImpl.h"

namespace Microsoft {
  namespace Reef {
    namespace Driver {
      namespace Bridge {
        private ref class ManagedLog {
          internal:
            static BridgeLogger^ LOGGER = BridgeLogger::GetLogger("<C++>");
        };

        ActiveContextClr2Java::ActiveContextClr2Java(JNIEnv *env, jobject jobjectActiveContext) {
          ManagedLog::LOGGER->LogStart("ActiveContextClr2Java::ActiveContextClr2Java");
          pin_ptr<JavaVM*> pJavaVm = &_jvm;
          if (env->GetJavaVM(pJavaVm) != 0) {
            ManagedLog::LOGGER->LogError("Failed to get JavaVM", nullptr);
          }

          _jobjectActiveContext = reinterpret_cast<jobject>(env->NewGlobalRef(jobjectActiveContext));

          jclass jclassActiveContext = env->GetObjectClass(_jobjectActiveContext);

          jfieldID jidContextId = env->GetFieldID(jclassActiveContext, "contextId", "Ljava/lang/String;");
          _jstringId = reinterpret_cast<jstring>(env->NewGlobalRef(env->GetObjectField(_jobjectActiveContext, jidContextId)));

          jfieldID jidEvaluatorId = env->GetFieldID(jclassActiveContext, "evaluatorId", "Ljava/lang/String;");
          _jstringEvaluatorId = (jstring)env->GetObjectField(_jobjectActiveContext, jidEvaluatorId);
          _jstringEvaluatorId = reinterpret_cast<jstring>(env->NewGlobalRef(_jstringEvaluatorId));

          ManagedLog::LOGGER->LogStop("ActiveContextClr2Java::ActiveContextClr2Java");
        }

        void ActiveContextClr2Java::SubmitTask(String^ taskConfigStr) {
          ManagedLog::LOGGER->LogStart("ActiveContextClr2Java::SubmitTask");
          JNIEnv *env = RetrieveEnv(_jvm);
          jclass jclassActiveContext = env->GetObjectClass (_jobjectActiveContext);
          jmethodID jmidSubmitTask = env->GetMethodID(jclassActiveContext, "submitTaskString", "(Ljava/lang/String;)V");

          if (jmidSubmitTask == NULL) {
            ManagedLog::LOGGER->Log("jmidSubmitTask is NULL");
            return;
          }
          env -> CallObjectMethod(
            _jobjectActiveContext,
            jmidSubmitTask,
            JavaStringFromManagedString(env, taskConfigStr));
          ManagedLog::LOGGER->LogStop("ActiveContextClr2Java::SubmitTask");
        }

        void ActiveContextClr2Java::OnError(String^ message) {
          JNIEnv *env = RetrieveEnv(_jvm);
          HandleClr2JavaError(env, message, _jobjectActiveContext);
        }

        void ActiveContextClr2Java::Close() {
          ManagedLog::LOGGER->LogStart("ActiveContextClr2Java::Close");
          JNIEnv *env = RetrieveEnv(_jvm);
          jclass jclassActiveContext = env->GetObjectClass (_jobjectActiveContext);
          jmethodID jmidClose = env->GetMethodID(jclassActiveContext, "close", "()V");

          if (jmidClose == NULL) {
            ManagedLog::LOGGER->Log("jmidClose is NULL");
            return;
          }
          env -> CallObjectMethod(
            _jobjectActiveContext,
            jmidClose);
          ManagedLog::LOGGER->LogStop("ActiveContextClr2Java::Close");
        }

        String^ ActiveContextClr2Java::GetId() {
          JNIEnv *env = RetrieveEnv(_jvm);
          return ManagedStringFromJavaString(env, _jstringId);
        }

        String^ ActiveContextClr2Java::GetEvaluatorId() {
          JNIEnv *env = RetrieveEnv(_jvm);
          return ManagedStringFromJavaString(env, _jstringEvaluatorId);
        }

        IEvaluatorDescriptor^ ActiveContextClr2Java::GetEvaluatorDescriptor() {
          ManagedLog::LOGGER->LogStart("ActiveContextClr2Java::GetEvaluatorDescriptor");
          return CommonUtilities::RetrieveEvaluatorDescriptor(_jobjectActiveContext, _jvm);
        }
      }
    }
  }
}