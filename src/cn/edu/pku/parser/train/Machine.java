package cn.edu.pku.parser.train;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class Machine {
    public Problem problem = new Problem();


    SolverType solver = SolverType.L2R_LR; // -s 0
    double C = 1.0;    // cost of constraints violation
    double eps = 0.01; // stopping criteria

    Parameter parameter = new Parameter(solver, C, eps);

    Feature[][] features;
}


//File modelFile = new File("model");
//model.save(modelFile);
// load model or use it directly
//model = Model.load(modelFile);