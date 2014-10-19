/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package snu.BDCS.SGD_Linear_Regression;

import com.microsoft.reef.examples.groupcomm.matmul.DenseVector;
import com.microsoft.reef.examples.groupcomm.matmul.Vector;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Scatter;
import com.microsoft.reef.task.Task;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ComputeTask Receives the partial matrix(row partitioned) it is
 * responsible for. Also receives one column vector per iteration and computes
 * the partial product of this vector with its assigned partial matrix The
 * partial product across all the compute tasks are concatenated by doing a
 * Reduce with Concat as the Reduce Function
 *
 * @author shravan
 */
public class ComputeTask implements Task {
    private final Logger logger = Logger.getLogger(ComputeTask.class
            .getName());
    private static final double RUNNING_RATE = 0.001;
    private static Vector theta = new DenseVector(6);
    /**
     * The Group Communication Operators that are needed by this task. These
     * will be injected into the constructor by TANG. The operators used here
     * are complementary to the ones used in the ControllerTask
     */
    Scatter.Receiver<Vector> scatterReceiver;
    Broadcast.Receiver<Vector> broadcastReceiver;
    Reduce.Sender<Vector> reduceSender;

    /**
     * This class is instantiated by TANG
     *
     * @param scatterReceiver   The receiver for the scatter operation
     * @param broadcastReceiver The receiver for the broadcast operation
     * @param reduceSender      The sender for the reduce operation
     */
    @Inject
    public ComputeTask(Scatter.Receiver<Vector> scatterReceiver,
                       Broadcast.Receiver<Vector> broadcastReceiver,
                       Reduce.Sender<Vector> reduceSender) {
        super();
        this.scatterReceiver = scatterReceiver;
        this.broadcastReceiver = broadcastReceiver;
        this.reduceSender = reduceSender;
    }

    @Override
    public byte[] call(byte[] memento) throws Exception {
        // Receive the partial matrix using which
        // we compute the dot products
        logger.log(Level.INFO, "Waiting for scatterReceive");
        System.out.print("Waiting for scatterReceive\n");
        List<Vector> exampleSet = scatterReceiver.receive();
        logger.log(Level.INFO, "Received: " + exampleSet);
        System.out.print("Received: " + exampleSet + "\n");

        if (!exampleSet.isEmpty()) {
            computeTheta(exampleSet);
            reduceSender.send(theta);
        } else {
            Vector zero = new DenseVector(6);
            for (int i=0; i<6; i++) zero.set(i, 0.0);
            reduceSender.send(zero);
        }
        return null;
    }

    private void computeTheta(List<Vector> exampleSet) {
        double temp = 0;
        Vector previous_theta;
        for (Vector example : exampleSet) {
            Vector next_theta = new DenseVector(6);
            for (int i = 0; i<6; i++) {
                previous_theta = theta;
                double hypothesis = previous_theta.dot(example);
                temp = (theta.get(i) - (RUNNING_RATE * (hypothesis - example.get(6)) * example.get(i)));
                System.out.print(theta.get(i)+" - "+RUNNING_RATE+" * ("+hypothesis+" - "+example.get(6)+") *"+example.get(i)+" = ");
                next_theta.set(i, temp);
                System.out.print(temp + " = ");
                System.out.print(next_theta.get(i)+"\n");
            }
            theta = next_theta;
            System.out.print("The result of computeTheta: " + theta + "\n");
        }
    }
}
