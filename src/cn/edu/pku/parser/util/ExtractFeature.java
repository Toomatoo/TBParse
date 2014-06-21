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
     * @return a feature vector of current stage
     */
    public Feature[] features(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen, int sumOffeatures,
                              HashMap<String, Integer> indexOfword) {
        ArrayList<Feature> f = new ArrayList<Feature>();

        int indexOffts = 0;
        /* Feature from stack */
        ArrayList<FeatureNode> stackF = getStackFeatures(stack, 3, indexOfword, indexOffts);
        f.addAll(stackF);
        indexOffts += stackF.size();

        /* Feature from queue in sentence */
        ArrayList<FeatureNode> stackQ = getQueueFeatures(sentence, 3, indexOfwordInsen, indexOfword, indexOffts);
        f.addAll(stackQ);
        indexOffts += stackQ.size();

        /* Feature from tree in sentence */
        ArrayList<FeatureNode> stackT = getTreeFeatures(stack, sentence, _sentence, 2, indexOfwordInsen, indexOfword, indexOffts);
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
     * @param index: feature index
     * @return
     */
    ArrayList<FeatureNode> getStackFeatures(Stack<Word> stack, int depth,
                                            HashMap<String, Integer> indexOfword,
                                            int index) {
        int base = index * indexOfword.size();
        ArrayList<FeatureNode> stackF = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        int _depth = depth;
        if(depth > stack.size())
            depth = stack.size();
        for(int i=0; i<depth; i++) {
            Word w = stack.get(stack.size() - 1 - i);
            if(indexOfword.containsKey(w.word.toLowerCase()))
                stackF.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
            else
                stackF.add(new FeatureNode(base+1, 1));
            base += indexOfword.size();
            if(indexOfword.containsKey(w.PoS))
                stackF.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
            else
                stackF.add(new FeatureNode(base+1, 1));
            base += indexOfword.size();
        }
        // complement the blank
        if(_depth > stack.size()) {
            for(int i=0; i<_depth-stack.size(); i++) {
                stackF.add(new FeatureNode(base+1, 1));
                base += indexOfword.size();
                stackF.add(new FeatureNode(base+1, 1));
                base += indexOfword.size();
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
     * @param index: feature index
     * @return
     */
    ArrayList<FeatureNode> getQueueFeatures(ArrayList<Word> sentence, int length, int indexOfwordInsen,
                                            HashMap<String, Integer> indexOfword,
                                            int index) {
        int base = index * indexOfword.size();
        ArrayList<FeatureNode> stackQ = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        int _length = length;
        if(length > sentence.size()-indexOfwordInsen)
            length = sentence.size()-indexOfwordInsen;

        for(int i=0; i<length; i++) {
            Word w = sentence.get(indexOfwordInsen+i);
            if(indexOfword.containsKey(w.word.toLowerCase()))
                stackQ.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
            else
                stackQ.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
            if(indexOfword.containsKey(w.PoS))
                stackQ.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
            else
                stackQ.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
        }

        // complement the blank
        if(_length > sentence.size()-indexOfwordInsen) {
            for(int i=0; i<_length-sentence.size()+indexOfwordInsen; i++) {
                stackQ.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                stackQ.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
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
     * @param index
     * @return
     */
    ArrayList<FeatureNode> getTreeFeatures(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int sum,
                                           int indexOfwordInsen, HashMap<String, Integer> indexOfword,
                                           int index) {
        int base = index * indexOfword.size();
        ArrayList<FeatureNode> stackT = new ArrayList<FeatureNode>();
        // Information from words and PoSs
        // Feature of Current Tree
        //  Queue: left and right children can be found
        //  Stack: left children can be found

        // For queue:
        //  Left
        ArrayList<Integer> indexofchild = new ArrayList<Integer>();
        for (int i = 0; i < sum; i++)
            indexofchild.add(-1);
        int sumofchild = 0;
        for (int i = 0; i < sentence.get(indexOfwordInsen).sons.size(); i++) {
            if (sumofchild >= sum)
                break;
            int idx = sentence.get(indexOfwordInsen).sons.get(i);
            if (idx < indexOfwordInsen) {
                indexofchild.set(sumofchild, idx);
                sumofchild++;
            }
        }
        for (int i = 0; i < sumofchild; i++) {
            Word w = _sentence.get(indexofchild.get(i));
            if(indexOfword.containsKey(w.word.toLowerCase()))
                stackT.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
            else
                stackT.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
            if(indexOfword.containsKey(w.PoS))
                stackT.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
            else
                stackT.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
        }
        // complement the blank
        if (sumofchild < sum) {
            for (int i = 0; i < sum - sumofchild; i++) {
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
            }
        }
        //  Right
        indexofchild = new ArrayList<Integer>();
        sumofchild = 0;
        for (int i = 0; i < sum; i++)
            indexofchild.add(-1);
        for (int i = sentence.get(indexOfwordInsen).sons.size() - 1; i >= 0; i--) {
            if (sumofchild >= sum)
                break;
            int idx = sentence.get(indexOfwordInsen).sons.get(i);
            if (idx > indexOfwordInsen) {
                indexofchild.set(sumofchild, idx);
                sumofchild++;
            }
        }
        for (int i = 0; i < sumofchild; i++) {
            Word w = _sentence.get(indexofchild.get(i));
            if(indexOfword.containsKey(w.word.toLowerCase()))
                stackT.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
            else
                stackT.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
            if(indexOfword.containsKey(w.PoS))
                stackT.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
            else
                stackT.add( new FeatureNode(base+1, 1) );
            base += indexOfword.size();
        }
        // complement the blank
        if (sumofchild < sum) {
            for (int i = 0; i < sum - sumofchild; i++) {
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
            }
        }

        // For Stack:
        if (!stack.isEmpty()) {
            // Left
            indexofchild = new ArrayList<Integer>();
            sumofchild = 0;
            Word w = stack.peek();
            for (int i = 0; i < sum; i++)
                indexofchild.add(-1);
            for (int i = 0; i < w.sons.size(); i++) {
                if (sumofchild >= sum)
                    break;
                int idx = w.sons.get(i);
                if (idx < indexOfwordInsen) {
                    indexofchild.set(sumofchild, idx);
                    sumofchild++;
                }
            }
            for (int i = 0; i < sumofchild; i++) {
                w = _sentence.get(indexofchild.get(i));
                if(indexOfword.containsKey(w.word.toLowerCase()))
                    stackT.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
                else
                    stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                if(indexOfword.containsKey(w.PoS))
                    stackT.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
                else
                    stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
            }
            // complement the blank
            if (sumofchild < sum) {
                for (int i = 0; i < sum - sumofchild; i++) {
                    stackT.add( new FeatureNode(base+1, 1) );
                    base += indexOfword.size();
                    stackT.add( new FeatureNode(base+1, 1) );
                    base += indexOfword.size();
                }
            }
            // Right
            indexofchild = new ArrayList<Integer>();
            sumofchild = 0;
            for (int i = 0; i < sum; i++)
                indexofchild.add(-1);
            for (int i = w.sons.size() - 1; i >= 0; i--) {
                if (sumofchild >= sum)
                    break;
                int idx = w.sons.get(i);
                if (idx > indexOfwordInsen) {
                    indexofchild.set(sumofchild, idx);
                    sumofchild++;
                }
            }
            for (int i = 0; i < sumofchild; i++) {
                w = _sentence.get(indexofchild.get(i));
                if(indexOfword.containsKey(w.word.toLowerCase()))
                    stackT.add( new FeatureNode(base + indexOfword.get(w.word.toLowerCase()), 1) );
                else
                    stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                if(indexOfword.containsKey(w.PoS))
                    stackT.add( new FeatureNode(base + indexOfword.get(w.PoS), 1) );
                else
                    stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
            }
            // complement the blank
            if (sumofchild < sum) {
                for (int i = 0; i < sum - sumofchild; i++) {
                    stackT.add( new FeatureNode(base+1, 1) );
                    base += indexOfword.size();
                    stackT.add( new FeatureNode(base+1, 1) );
                    base += indexOfword.size();
                }
            }
        } else {
            for (int i = 0; i < sum * 2; i++) {
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
                stackT.add( new FeatureNode(base+1, 1) );
                base += indexOfword.size();
            }
        }

        return stackT;
    }
}