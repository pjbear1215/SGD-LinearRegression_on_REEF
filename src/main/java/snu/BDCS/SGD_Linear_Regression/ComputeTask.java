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
    private static final double RUNNING_RATE = 1;
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
        System.out.print("Received: \n" + exampleSet);

        // Compute partial product Ax
        computeTheta(exampleSet);
        // Send up the aggregation(concatenation) tree
        // to the controller task
        reduceSender.send(theta);
        return null;
    }

    private void computeTheta(List<Vector> exampleSet) {
        int i = 0;
        for (Vector example : exampleSet) {
            for (; i<6; i++) {
                theta.set(i, (theta.get(i) - (RUNNING_RATE * (theta.dot(example) - example.get(6)))));
            }
        }
    }
}
