package cn.edu.pku.parser.main;

import cn.edu.pku.parser.train.WordTrain;
import de.bwaldvogel.liblinear.*;

import java.util.Stack;


public class Main {
    public static void main(String []args) throws Exception {
        /*
        Problem problem = new Problem();

        problem.l = 1; // number of training examples
        problem.n = 2; // number of features (templates)

        FeatureNode f1 = new FeatureNode(1, 3);
        FeatureNode f2 = new FeatureNode(2, 3);
        Feature [][] f = new Feature[1][2]; // [1][1] -> n = 2
        f[0][0] = f1;
        f[0][1] = f2;
        problem.x = f; // feature nodes
        double []y = {3};
        problem.y = y; // target values
        problem.bias = -1;

        SolverType solver = SolverType.L2R_L2LOSS_SVC; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);

        Model model = Linear.train(problem, parameter);
        //File modelFile = new File("model");
        //model.save(modelFile);
        // load model or use it directly
        //model = Model.load(modelFile);
        Feature[] instance = { new FeatureNode(1, 4), new FeatureNode(2, 2) };
        double prediction = Linear.predict(model, instance);
        System.out.println(prediction);
        */
        String flname = "./data/trn.conll08";
        WordTrain t = new WordTrain(flname);
        t.train();
    }

}



