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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ControllerTask Controls the matrix multiplication task Splits up the
 * input matrix into parts(row-wise) and scatters them amongst the compute
 * tasks Broadcasts each column vector Receives the reduced(concatenated)
 * partial products as the output vector
 *
 * @author shravan
 */
public class ControllerTask implements Task {

    private final Logger logger = Logger.getLogger(ControllerTask.class.getName());

    /**
     * The Group Communication Operators that are needed by this task. These
     * will be injected into the constructor by TANG. The operators used here
     * are complementary to the ones used in the ComputeTask
     */
    Scatter.Sender<Vector> scatterSender;
    Broadcast.Sender<Vector> broadcastSender;
    Reduce.Receiver<Vector> reduceReceiver;

    // The matrices
    List<Vector> exampleSet;
    private static Vector theta = new DenseVector(6);
    // We compute AX'

    /**
     * This class is instantiated by TANG
     *
     * @param scatterSender   The sender for the scatter operation
     * @param broadcastSender The sender for the broadcast operation
     * @param reduceReceiver  The receiver for the reduce operation
     */
    @Inject
    public ControllerTask(Scatter.Sender<Vector> scatterSender,
                          Broadcast.Sender<Vector> broadcastSender,
                          Reduce.Receiver<Vector> reduceReceiver) {
        super();
        this.scatterSender = scatterSender;
        this.broadcastSender = broadcastSender;
        this.reduceReceiver = reduceReceiver;

        // For now initialize the matrix
        // TODO: Read from disk/hdfs
        double[][] matrix = {
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.125, 0.11},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.15, 0.27},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.175, 0.47},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.2, 0.78},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.225, 1.18},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.25, 1.82},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.275, 2.61},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.3, 3.76},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.325, 4.99},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.35, 7.16},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.375, 11.93},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.4, 20.11},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.425, 32.75},
                {-2.3, 0.568, 4.78, 3.99, 3.17, 0.45, 49.49},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.125, 0.04},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.15, 0.17},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.175, 0.37},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.2, 0.66},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.225, 1.06},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.25, 1.59},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.275, 2.33},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.3, 3.29},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.325, 4.61},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.35, 7.11},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.375, 11.99},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.4, 21.09},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.425, 35.01},
                {-2.3, 0.569, 4.78, 3.04, 3.64, 0.45, 51.8},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.125, 0.09},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.15, 0.29},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.175, 0.56},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.2, 0.86},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.225, 1.31},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.25, 1.99},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.275, 2.94},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.3, 4.21},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.325, 5.54},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.35, 8.25},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.375, 13.08},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.4, 21.4},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.425, 33.14},
                {-2.3, 0.565, 4.78, 5.35, 2.76, 0.45, 50.14},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.125, 0.2},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.15, 0.35},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.175, 0.65},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.2, 0.93},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.225, 1.37},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.25, 1.97},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.275, 2.83},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.3, 3.99},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.325, 5.19},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.35, 8.03},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.375, 12.86},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.4, 21.51},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.425, 33.97},
                {-2.3, 0.564, 5.1, 3.95, 3.53, 0.45, 50.36},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.125, 0.2},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.15, 0.35},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.175, 0.65},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.2, 0.93},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.225, 1.37},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.25, 1.97},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.275, 2.83},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.3, 3.99},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.325, 5.19},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.35, 8.03},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.375, 12.86},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.4, 21.51},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.425, 33.97},
                {-2.4, 0.574, 4.36, 3.96, 2.76, 0.45, 50.36},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.125, 0.12},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.15, 0.26},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.175, 0.43},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.2, 0.69},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.225, 1.09},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.25, 1.67},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.275, 2.46},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.3, 3.43},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.325, 4.62},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.35, 6.86},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.375, 11.56},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.4, 20.63},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.425, 34.5},
                {-2.4, 0.568, 4.34, 2.98, 3.15, 0.45, 54.23},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.125, 0.28},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.15, 0.44},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.175, 0.7},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.2, 1.07},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.225, 1.57},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.25, 2.23},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.275, 3.09},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.3, 4.09},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.325, 5.82},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.35, 8.28},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.375, 12.8},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.4, 20.41},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.425, 32.34},
                {-2.3, 0.562, 5.14, 4.95, 3.17, 0.45, 47.29},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.125, 0.2},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.15, 0.38},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.175, 0.64},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.2, 0.97},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.225, 1.36},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.25, 1.98},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.275, 2.91},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.3, 4.35},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.325, 5.79},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.35, 8.04},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.375, 12.15},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.4, 19.18},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.425, 30.09},
                {-2.4, 0.585, 4.78, 3.84, 3.32, 0.45, 44.38},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.125, 0.15},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.15, 0.32},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.175, 0.55},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.2, 0.86},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.225, 1.24},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.25, 1.76},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.275, 2.49},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.3, 3.45},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.325, 4.83},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.35, 7.37},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.375, 12.76},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.4, 21.99},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.425, 35.64},
                {-2.2, 0.546, 4.78, 4.13, 3.07, 0.45, 53.07},
                {0, 0.565, 4.77, 3.99, 3.15, 0.125, 0.11},
                {0, 0.565, 4.77, 3.99, 3.15, 0.15, 0.24},
                {0, 0.565, 4.77, 3.99, 3.15, 0.175, 0.49},
                {0, 0.565, 4.77, 3.99, 3.15, 0.2, 0.79},
                {0, 0.565, 4.77, 3.99, 3.15, 0.225, 1.28},
                {0, 0.565, 4.77, 3.99, 3.15, 0.25, 1.96},
                {0, 0.565, 4.77, 3.99, 3.15, 0.275, 2.88},
                {0, 0.565, 4.77, 3.99, 3.15, 0.3, 4.14},
                {0, 0.565, 4.77, 3.99, 3.15, 0.325, 5.96},
                {0, 0.565, 4.77, 3.99, 3.15, 0.35, 9.07},
                {0, 0.565, 4.77, 3.99, 3.15, 0.375, 14.93},
                {0, 0.565, 4.77, 3.99, 3.15, 0.4, 24.13},
                {0, 0.565, 4.77, 3.99, 3.15, 0.425, 38.12},
                {0, 0.565, 4.77, 3.99, 3.15, 0.45, 55.44},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.125, 0.07},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.15, 0.18},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.175, 0.4},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.2, 0.7},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.225, 1.14},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.25, 1.83},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.275, 2.77},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.3, 4.12},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.325, 5.41},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.35, 7.87},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.375, 12.71},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.4, 21.02},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.425, 34.58},
                {-5, 0.565, 4.77, 3.99, 3.15, 0.45, 51.77},
                {0, 0.565, 5.1, 3.94, 3.51, 0.125, 0.08},
                {0, 0.565, 5.1, 3.94, 3.51, 0.15, 0.26},
                {0, 0.565, 5.1, 3.94, 3.51, 0.175, 0.5},
                {0, 0.565, 5.1, 3.94, 3.51, 0.2, 0.83},
                {0, 0.565, 5.1, 3.94, 3.51, 0.225, 1.28},
                {0, 0.565, 5.1, 3.94, 3.51, 0.25, 1.9},
                {0, 0.565, 5.1, 3.94, 3.51, 0.275, 2.68},
                {0, 0.565, 5.1, 3.94, 3.51, 0.3, 3.76},
                {0, 0.565, 5.1, 3.94, 3.51, 0.325, 5.57},
                {0, 0.565, 5.1, 3.94, 3.51, 0.35, 8.76},
                {0, 0.565, 5.1, 3.94, 3.51, 0.375, 14.24},
                {0, 0.565, 5.1, 3.94, 3.51, 0.4, 23.05},
                {0, 0.565, 5.1, 3.94, 3.51, 0.425, 35.46},
                {0, 0.565, 5.1, 3.94, 3.51, 0.45, 51.99},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.125, 0.08},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.15, 0.24},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.175, 0.45},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.2, 0.77},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.225, 1.19},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.25, 1.76},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.275, 2.59},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.3, 3.85},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.325, 5.27},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.35, 7.74},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.375, 12.4},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.4, 20.91},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.425, 33.23},
                {-5, 0.565, 5.1, 3.94, 3.51, 0.45, 49.14},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.125, 0.08},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.15, 0.25},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.175, 0.46},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.2, 0.75},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.225, 1.11},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.25, 1.57},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.275, 2.17},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.3, 2.98},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.325, 4.42},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.35, 7.84},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.375, 14.11},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.4, 24.14},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.425, 37.95},
                {-2.3, 0.53, 5.11, 3.69, 3.51, 0.45, 55.17},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.125, 0.1},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.15, 0.23},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.175, 0.47},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.2, 0.76},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.225, 1.15},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.25, 1.65},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.275, 2.28},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.3, 3.09},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.325, 4.41},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.35, 7.51},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.375, 13.77},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.4, 23.96},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.425, 37.38},
                {-2.3, 0.53, 4.76, 3.68, 3.16, 0.45, 56.46},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.125, 0.05},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.15, 0.17},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.175, 0.35},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.2, 0.63},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.225, 1.01},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.25, 1.43},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.275, 2.05},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.3, 2.73},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.325, 3.87},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.35, 7.19},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.375, 13.96},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.4, 25.18},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.425, 41.34},
                {-2.3, 0.53, 4.34, 2.81, 3.15, 0.45, 62.42},
                {0, 0.6, 4.78, 4.24, 3.15, 0.125, 0.03},
                {0, 0.6, 4.78, 4.24, 3.15, 0.15, 0.18},
                {0, 0.6, 4.78, 4.24, 3.15, 0.175, 0.4},
                {0, 0.6, 4.78, 4.24, 3.15, 0.2, 0.73},
                {0, 0.6, 4.78, 4.24, 3.15, 0.225, 1.3},
                {0, 0.6, 4.78, 4.24, 3.15, 0.25, 2.16},
                {0, 0.6, 4.78, 4.24, 3.15, 0.275, 3.35},
                {0, 0.6, 4.78, 4.24, 3.15, 0.3, 5.06},
                {0, 0.6, 4.78, 4.24, 3.15, 0.325, 7.14},
                {0, 0.6, 4.78, 4.24, 3.15, 0.35, 10.36},
                {0, 0.6, 4.78, 4.24, 3.15, 0.375, 15.25},
                {0, 0.6, 4.78, 4.24, 3.15, 0.4, 23.15},
                {0, 0.6, 4.78, 4.24, 3.15, 0.425, 34.62},
                {0, 0.6, 4.78, 4.24, 3.15, 0.45, 51.5},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.125, 0.06},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.15, 0.15},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.175, 0.34},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.2, 0.63},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.225, 1.13},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.25, 1.85},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.275, 2.84},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.3, 4.34},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.325, 6.2},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.35, 8.62},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.375, 12.49},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.4, 20.41},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.425, 32.46},
                {-5, 0.6, 4.78, 4.24, 3.15, 0.45, 50.94},
                {0, 0.53, 4.78, 3.75, 3.15, 0.125, 0.16},
                {0, 0.53, 4.78, 3.75, 3.15, 0.15, 0.32},
                {0, 0.53, 4.78, 3.75, 3.15, 0.175, 0.59},
                {0, 0.53, 4.78, 3.75, 3.15, 0.2, 0.92},
                {0, 0.53, 4.78, 3.75, 3.15, 0.225, 1.37},
                {0, 0.53, 4.78, 3.75, 3.15, 0.25, 1.94},
                {0, 0.53, 4.78, 3.75, 3.15, 0.275, 2.62},
                {0, 0.53, 4.78, 3.75, 3.15, 0.3, 3.7},
                {0, 0.53, 4.78, 3.75, 3.15, 0.325, 5.45},
                {0, 0.53, 4.78, 3.75, 3.15, 0.35, 9.45},
                {0, 0.53, 4.78, 3.75, 3.15, 0.375, 16.31},
                {0, 0.53, 4.78, 3.75, 3.15, 0.4, 27.34},
                {0, 0.53, 4.78, 3.75, 3.15, 0.425, 41.77},
                {0, 0.53, 4.78, 3.75, 3.15, 0.45, 60.85},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.125, 0.09},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.15, 0.24},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.175, 0.47},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.2, 0.78},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.225, 1.21},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.25, 1.85},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.275, 2.62},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.3, 3.69},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.325, 5.07},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.35, 7.95},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.375, 13.73},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.4, 23.55},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.425, 37.14},
                {-5, 0.53, 4.78, 3.75, 3.15, 0.45, 55.87},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.125, 0.01},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.15, 0.16},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.175, 0.39},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.2, 0.73},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.225, 1.24},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.25, 1.96},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.275, 3.04},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.3, 4.46},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.325, 6.31},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.35, 8.68},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.375, 12.39},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.4, 20.14},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.425, 31.77},
                {-2.3, 0.6, 5.1, 4.17, 3.51, 0.45, 47.13},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.125, 0.04},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.15, 0.17},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.175, 0.36},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.2, 0.64},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.225, 1.02},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.25, 1.62},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.275, 2.63},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.3, 4.15},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.325, 6},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.35, 8.47},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.375, 12.27},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.4, 19.59},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.425, 30.48},
                {-2.3, 0.6, 4.34, 4.23, 2.73, 0.45, 46.66}
        };

        // Convert matrix into a list of row vectors
        exampleSet = new ArrayList<>(matrix.length);
        for (int i = 0; i < matrix.length; i++) {
            Vector rowi = new DenseVector(7);
            for (int j = 0; j < matrix[i].length; j++) {
                rowi.set(j, matrix[i][j]);
            }
            exampleSet.add(rowi);
        }
    }

    /**
     * Computes AX'
     */
    @Override
    public byte[] call(byte[] memento) throws Exception {
        // Scatter the matrix exampleSet
        logger.log(Level.INFO, "Scattering Data");
        System.out.print("Scattering Data\n");
        scatterSender.send(exampleSet);
        logger.log(Level.INFO, "Finished Scattering Data");
        System.out.print( "Finished Scattering Data\n");
        Vector result = reduceReceiver.reduce();
        // Accumulate the result
        String resStr = resultString(result);
        return resStr.getBytes();
    }

    /**
     * Construct the display string and send it to the driver
     *
     * @param result = AX'
     * @return exampleSet string indicating the matrices being multiplied and the result
     */
    private String resultString(Vector result) {
        StringBuilder sb = new StringBuilder();

        double[] res = new double[result.size()];
        for (int i = 0; i < result.size(); i++) {
                res[i] = result.get(i);
        }

        for (int i = 0; i < 6; i++) {
            sb.append("Theta " + i + ": " + res[i] + "\n");
        }

        return sb.toString();
    }
}
