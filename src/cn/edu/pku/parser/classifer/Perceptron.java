/**
 * @project Semantic Role Labeling
 * @package perceptron
 * @author Siyuan Liu
 * @data Apr 29, 2014
 * @copyright 2014
 * @version 1.0
 */
package cn.edu.pku.parser.classifer;

import java.util.ArrayList;
import java.util.HashMap;

import cn.edu.pku.parser.util.Gen;

/**
 * @CLassname Perceptron
 * @author Siyuan Liu
 */

public class Perceptron {
    /* Templates of features */
    int sumoftemplates;
    /* Special labels for this perceptron machine */
    ArrayList<String> labels = new ArrayList<String>();
    /*
     * parameters for this perceptron machine
     * 	the first layer is for label
     * 	the second layer is for template
     * 	the third layer is for feature
    */
    ArrayList<ArrayList<ArrayList<Double>>> param = new ArrayList<ArrayList<ArrayList<Double>>>();
    //ArrayList<ArrayList<String>> paramStr = new ArrayList<ArrayList<String>>();
    ArrayList<HashMap<String, Integer>> paramStr = new ArrayList<HashMap<String, Integer>>();
    /* Increment or Decrement Unit */
    final static double var = 1;
    /* Gen */
    Gen g;
    /**
     *
     * @param sumoftemplates: <Integer>sum of the templates for the perceptron
     * @param labels: <ArrayList<String>>labels used in this perceptron
     */
    public Perceptron(int sumoftemplates, ArrayList<String> labels, Gen g) {
        // Default setting:
        //	The features are denoted in String
        this.g = g;
        this.sumoftemplates = sumoftemplates;
        this.labels.addAll(labels);
        for(int i=0; i<labels.size(); i++) {
            // For each label
            param.add(new ArrayList<ArrayList<Double>>());

            for(int j=0; j<sumoftemplates; j++) {
                // For each template
                param.get(i).add(new ArrayList<Double>());
            }
        }

        for(int i=0; i<sumoftemplates; i++)
            paramStr.add(new HashMap<String, Integer>());
    }


    /**
     * Train the machine adjusting the parameters
     * @param featureLabel: Array of Strings
     * 		eg. PoS tagging: current__w__prefix__pre__sufix__su
     */
    public void trainMachine(ArrayList<String> featureLabel, double precision) {
        // Train the machine iteratively
        //	until the parameter do not change a lot

        // Parse each 'featureLabel' to locate the param.
        //	Then call 'trainIteration'
        while(true) {
            int error = trainIteration(featureLabel);
            System.out.println(error);
            if(((double)error) / featureLabel.size() <= precision)
                break;
        }
    }


    /**
     * Each iteration on the training data
     */
    public int trainIteration(ArrayList<String> featureLabel) {
        // For each iteration
        //	run the training process on the whole training data

        //	for each training data:
        //		find the arg max Label
        //		examine the Label to adjust the parameters
        int error = 0;
        String pre_label = "";
// Need to modify 'featureLabel'
        for(int i=0; i<featureLabel.size(); i++) {
            if(featureLabel.get(i).equals("")) {
                pre_label = "";
                continue;
            }
            // Split from the argument to get features and label
            String []str = featureLabel.get(i).split("__");
            ArrayList<String> features = new ArrayList<String>();
            for(int j=0; j<sumoftemplates; j++)
                features.add(str[j*2+1]);
            String label = str[sumoftemplates*2+1];

            // Training this data
            //	Store the features and get the index of them
            //	Calculate the SCOREs and pick out the maximum
            //		Hint: define the features structure carefully
            // 	Check if correct
            for(int fea=0; fea<sumoftemplates; fea++) {
                // Check if the template[fea] contains the feature
                if(!paramStr.get(fea).containsKey(features.get(fea))) {
                    // Add new features into templates
                    paramStr.get(fea).put(features.get(fea), paramStr.get(fea).size());
                    for(int l=0; l<labels.size(); l++)
                        param.get(l).get(fea).add((double) 0);
                }
            }

            // Calculate the SCOREs and pick out the maximum
            int indexofMaxLabel = 0;
            double tmpMaxSCORE = -1111111111;

            ArrayList<String> genlabels = new ArrayList<String>();
            if(g != null)
                genlabels = g.ChunkGen(labels, pre_label);
            else
                genlabels.addAll(labels);
            for(int l=0; l<labels.size(); l++) {
                if(!genlabels.contains(labels.get(l)))
                    continue;
                double SCORE = 0;
                for(int tplate=0; tplate<sumoftemplates; tplate++) {
                    SCORE += param.get(l).get(tplate).get(
                            paramStr.get(tplate).get(features.get(tplate))
                    );
                }

                if(SCORE > tmpMaxSCORE) {
                    tmpMaxSCORE = SCORE;
                    indexofMaxLabel = l;
                }
                else if(SCORE <= -1111111111)
                    System.out.println("Error SCORE!");
            }

            // Check if correct
            if(indexofMaxLabel != labels.indexOf(label)) {
                error ++;

                for(int tplate=0; tplate<sumoftemplates; tplate++) {
                    double cur1 = param.get(indexofMaxLabel).get(tplate).get(
                            paramStr.get(tplate).get(features.get(tplate))
                    );
                    double cur2 = param.get(labels.indexOf(label)).get(tplate).get(
                            paramStr.get(tplate).get(features.get(tplate))
                    );
                    param.get(indexofMaxLabel).get(tplate).set(
                            paramStr.get(tplate).get(features.get(tplate)),
                            cur1 - var);
                    param.get(labels.indexOf(label)).get(tplate).set(
                            paramStr.get(tplate).get(features.get(tplate)),
                            cur2 + var);
                }
            }

            pre_label = label;
        }

        return error;
    }


    /**
     *
     */
    public String labelofInstance(String feature, String pre_label) {
        // Split from the argument to get features and label
        String []str = feature.split("__");
        ArrayList<String> features = new ArrayList<String>();
        for(int j=0; j<sumoftemplates; j++)
            features.add(str[j*2+1]);

        // Calculate the SCOREs and pick out the maximum
        int indexofMaxLabel = 0;
        double tmpMaxSCORE = -1111111111;

        ArrayList<String> genlabels = new ArrayList<String>();
        if(g != null)
            genlabels = g.ChunkGen(labels, pre_label);
        else
            genlabels.addAll(labels);
        for(int l=0; l<labels.size(); l++) {
            if(!genlabels.contains(labels.get(l)))
                continue;
            double SCORE = 0;
            for(int tplate=0; tplate<sumoftemplates; tplate++) {
                if(paramStr.get(tplate).containsKey(features.get(tplate)))
                    SCORE += param.get(l).get(tplate).get(
                            paramStr.get(tplate).get(features.get(tplate))
                    );
            }

            if(SCORE > tmpMaxSCORE) {
                tmpMaxSCORE = SCORE;
                indexofMaxLabel = l;
            }
            else if(SCORE <= -1111111111)
                System.out.println("Error SCORE!");
        }

        return labels.get(indexofMaxLabel);
    }

}
