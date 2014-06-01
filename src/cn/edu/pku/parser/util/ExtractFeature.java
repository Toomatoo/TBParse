package cn.edu.pku.parser.util;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class ExtractFeature {
    /**
     *
     * @param stack: stack of current sentence
     * @param sentence: current sentence
     * @param indexOfwordInsen: index of word in sentence
     * @param sumOffeatures: sum of feature templates
     * @param indexOfword: used to get the index of a word
     * @param indexOfPoS: used to get the index of a PoS
     * @return a feature vector of current stage
     */
    public Feature[] features(Stack<Word> stack, ArrayList<Word> sentence, int indexOfwordInsen, int sumOffeatures,
                              HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        ArrayList<Feature> f = new ArrayList<Feature>();

        int indexOffts = 0;
        /* Feature from stack */
        ArrayList<FeatureNode> stackF = getStackFeatures(stack, 3, indexOfword, indexOfPoS, indexOffts);
        f.addAll(stackF);
        indexOffts += stackF.size();
        /* Feature from queue in sentence */
        ArrayList<FeatureNode> stackQ = getQueueFeatures(sentence, 3, indexOfwordInsen, indexOfword, indexOfPoS, indexOffts);
        f.addAll(stackQ);
        indexOffts += stackQ.size();
        /* Feature from tree in sentence */
        ArrayList<FeatureNode> stackT = getTreeFeatures(stack, sentence, 2, indexOfwordInsen, indexOfword, indexOfPoS, indexOffts);
        f.addAll(stackT);
        indexOffts += stackT.size();

        Feature[] fts = new Feature[f.size()];
        for(int i=0; i<fts.length; i++)
            fts[i] = f.get(i);
        return fts;
    }


    /**
     *
     * @param stack: stack of current stage
     * @param depth: depth commanded
     * @param indexOfword: get word index
     * @param indexOfPoS: get PoS index
     * @param index: feature index
     * @return
     */
    ArrayList<FeatureNode> getStackFeatures(Stack<Word> stack, int depth,
                                            HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS,
                                            int index) {
        ArrayList<FeatureNode> stackF = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        int _depth = depth;
        if(depth > stack.size())
            depth = stack.size();
        for(int i=0; i<depth; i++) {
            Word w = stack.get(stack.size() - 1 - i);
//System.out.println(w.word);
            stackF.add( new FeatureNode(indexOfword.get(w.word), 1) );
            stackF.add( new FeatureNode(indexOfPoS.get(w.PoS), 1) );
        }
        // complement the blank
        if(_depth > stack.size()) {
            for(int i=0; i<_depth-stack.size(); i++) {
                stackF.add(new FeatureNode(0, 1));
                stackF.add(new FeatureNode(0, 1));
            }
        }
        return stackF;
    }

    /**
     *
     * @param sentence: current sentence
     * @param length: length commanded
     * @param indexOfwordInsen: head position of sentence
     * @param indexOfword: get word index
     * @param indexOfPoS: get PoS index
     * @param index: feature index
     * @return
     */
    ArrayList<FeatureNode> getQueueFeatures(ArrayList<Word> sentence, int length, int indexOfwordInsen,
                                            HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS,
                                            int index) {
        int indexHere = index;
        ArrayList<FeatureNode> stackQ = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        int _length = length;
        if(length > sentence.size()-indexOfwordInsen)
            length = sentence.size()-indexOfwordInsen;

        for(int i=0; i<length; i++) {
            Word w = sentence.get(indexOfwordInsen+i);
            stackQ.add( new FeatureNode(indexOfword.get(w.word), 1) );
            indexHere ++;
            stackQ.add( new FeatureNode(indexOfPoS.get(w.PoS), 1) );
            indexHere ++;
        }

        // complement the blank
        if(_length > sentence.size()-indexOfwordInsen) {
            for(int i=0; i<_length-sentence.size()+indexOfwordInsen; i++) {
                stackQ.add( new FeatureNode(0, 1) );
                indexHere ++;
                stackQ.add( new FeatureNode(0, 1) );
                indexHere ++;
            }
        }
        return stackQ;
    }

    /**
     *
     * @param stack
     * @param sentence
     * @param sum: sum of children commanded
     * @param indexOfwordInsen
     * @param indexOfword
     * @param indexOfPoS
     * @param index
     * @return
     */
    ArrayList<FeatureNode> getTreeFeatures(Stack<Word> stack, ArrayList<Word> sentence, int sum, int indexOfwordInsen,
                                           HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS,
                                           int index) {
        ArrayList<FeatureNode> stackT = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        // Feature of Current Tree
        //  Queue: left and right children can be found
        //  Stack: left children can be found

        // For queue:
        //  Left
        ArrayList<Integer> indexofchild = new ArrayList<Integer>();
        for(int i=0; i<sum; i++)
            indexofchild.add(-1);
        int sumofchild = 0;
        for(int i=0; i<indexOfwordInsen; i++) {
            if(sumofchild >= sum)
                break;
            if(sentence.get(i).head == indexOfwordInsen) {
                indexofchild.set(sumofchild, i);
                sumofchild ++;
            }
        }
        for(int i=0; i<sumofchild; i++) {
            Word w = sentence.get(indexofchild.get(i));
            stackT.add( new FeatureNode(indexOfword.get(w.word), 1) );
            stackT.add( new FeatureNode(indexOfPoS.get(w.PoS), 1) );
        }
        // complement the blank
        if(sumofchild < sum) {
            for(int i=0; i<sum-sumofchild; i++) {
                stackT.add( new FeatureNode(0, 1) );
                stackT.add( new FeatureNode(0, 1) );
            }
        }
        //  Right
        indexofchild = new ArrayList<Integer>();
        sumofchild = 0;
        for(int i=0; i<sum; i++)
            indexofchild.add(-1);
        for(int i=indexOfwordInsen+1; i<sentence.size(); i++) {
            if(sumofchild >= sum)
                break;
            if(sentence.get(i).head == indexOfwordInsen) {
                indexofchild.set(sumofchild, i);
                sumofchild ++;
            }
        }
        for(int i=0; i<sumofchild; i++) {
            Word w = sentence.get(indexofchild.get(i));
            stackT.add( new FeatureNode(indexOfword.get(w.word), 1) );
            stackT.add( new FeatureNode(indexOfPoS.get(w.PoS), 1) );
        }
        // complement the blank
        if(sumofchild < sum) {
            for(int i=0; i<sum-sumofchild; i++) {
                stackT.add( new FeatureNode(0, 1) );
                stackT.add( new FeatureNode(0, 1) );
            }
        }
        // For Stack: Left
        if(!stack.isEmpty()) {
            indexofchild = new ArrayList<Integer>();
            sumofchild = 0;
            Word w = stack.peek();
            for (int i = 0; i < sum; i++)
                indexofchild.add(-1);
            for (int i = 0; i < w.index; i++) {
                if (sumofchild >= sum)
                    break;
                if (sentence.get(i).head == w.index) {
                    indexofchild.set(sumofchild, i);
                    sumofchild++;
                }
            }
            for (int i = 0; i < sumofchild; i++) {
                w = sentence.get(indexofchild.get(i));
                stackT.add(new FeatureNode(indexOfword.get(w.word), 1));
                stackT.add(new FeatureNode(indexOfPoS.get(w.PoS), 1));
            }
            // complement the blank
            if(sumofchild < sum) {
                for(int i=0; i<sum-sumofchild; i++) {
                    stackT.add( new FeatureNode(0, 1) );
                    stackT.add( new FeatureNode(0, 1) );
                }
            }
        }
        else {
            for(int i=0; i<sum; i++) {
                stackT.add( new FeatureNode(0, 1) );
                stackT.add( new FeatureNode(0, 1) );
            }
        }

        return stackT;
    }
}