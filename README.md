SGD-LinearRegression_on_REEF
============================

Due to some implementation issues, I rather used an array for the data set instead of using data loading API.
The contollerTask scatters the whole training example set across computeTasks via scatter function.
Each computeTask locally adjusts the weight of each feature using SGD algorithm with received data set.
Once they finish, they send a vector of the weights computed above to the ControllerTask.
The controllerTask calculates the average of the recieved vectors from computeTasks and uses them to update the global weights.

Experimental Results
=============================
Data set is from Yacht Hydrodynamics Data Set. (https://archive.ics.uci.edu/ml/datasets/Yacht+Hydrodynamics#)

-----------------------------
Running Rate = 0.1

Theta 0: -1.4331202452429708E69

Theta 1: 3.3020126091645173E68

Theta 2: 2.9661729149974625E69

Theta 3: 2.2926804850070285E69

Theta 4: 1.9692267246670973E69

Theta 5: 2.210287908880366E68

-----------------------------
Running Rate = 0.01

Theta 0: -0.33480879778217343

Theta 1: 0.15505105931108795

Theta 2: 1.3387828096238306

Theta 3: 1.0209064599302928

Theta 4: 0.9024773598627395

Theta 5: 1.438839104668696

-----------------------------
Running Rate = 0.001

Theta 0: -0.3440739965792709

Theta 1: 0.11293872523659215

Theta 2: 0.951982906222438

Theta 3: 0.7516462974181289

Theta 4: 0.6475783517884577

Theta 5: 0.19539912437635384

-----------------------------
Running Rate = 0.0001

Theta 0: -0.18790064240239604

Theta 1: 0.046675548077507435

Theta 2: 0.39619552000552605

Theta 3: 0.32411633471642265

Theta 4: 0.2657375799245605

Theta 5: 0.036608024333773914
