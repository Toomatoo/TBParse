package cn.edu.pku.parser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class EFstemmed {
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
    public String features(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen, int sumOffeatures,
                           HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String f = "";

        /* Feature from stack */
        String stackF = getStackFeatures(stack, 4, indexOfword, indexOfPoS);//1-2
        f = stackF;

        /* Feature from queue in sentence */
        String stackQ = getQueueFeatures(sentence, 4, indexOfwordInsen, indexOfword, indexOfPoS);//1-2
        f = f + stackQ;

        /* Feature from tree in sentence */
        String stackT = getTreeFeatures(stack, sentence, _sentence, 3, indexOfwordInsen, indexOfword, indexOfPoS);//1-8
        f = f + stackT;

        /* Feature from distance information */
        String stackD = getDistanceFeatures(stack, sentence, _sentence, indexOfwordInsen, indexOfword, indexOfPoS);//1
        f += stackD;

        /* Feature from numbers of sons */
        String stackS = getSonFeatures(stack, sentence, _sentence, indexOfwordInsen, indexOfword, indexOfPoS);//4
        f += stackS;

        return f;
    }

    /**
     *
     * @param stack: stack of current stage
     * @param depth: depth commanded
     * @param indexOfword: get word index
     * @param indexOfPoS: get PoS index
     * @return
     */
    String getStackFeatures(Stack<Word> stack, int depth,
                            HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS
    ) {
        String stackF = "";
        // Information from words and PoSs
        int _depth = depth;
        if(depth > stack.size())
            depth = stack.size();
        for(int i=0; i<depth; i++) {
            Word w = stack.get(stack.size() - 1 - i);
//System.out.println(w.word);
            Stemmer s = new Stemmer();
            String str = w.word.toLowerCase();
            s.add(str.toCharArray(), str.length());
            s.stem();
            stackF += "stackWord" + "__" + s.toString() + "__" + "stackPoS" + "__" + w.PoS + "__";
        }
        // complement the blank
        if(_depth > stack.size()) {
            for(int i=0; i<_depth-stack.size(); i++) {
                stackF += "stackWord" + "__" + "%%NULL%%" + "__" + "stackPoS" + "__" + "%%NULL%%" + "__";
            }
        }
        return stackF;
    }

    String getQueueFeatures(ArrayList<Word> sentence, int length, int indexOfwordInsen,
                            HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS
    ) {
        String stackQ = "";
        // Information from words and PoSs
        int _length = length;
        if(length > sentence.size()-indexOfwordInsen)
            length = sentence.size()-indexOfwordInsen;

        for(int i=0; i<length; i++) {
            Word w = sentence.get(indexOfwordInsen+i);
            Stemmer s = new Stemmer();
            String str = w.word.toLowerCase();
            s.add(str.toCharArray(), str.length());
            s.stem();
            stackQ += "queueWord" + "__" + s.toString() + "__" + "queuePoS" + "__" + w.PoS + "__";
        }

        // complement the blank
        if(_length > sentence.size()-indexOfwordInsen) {
            for(int i=0; i<_length-sentence.size()+indexOfwordInsen; i++) {
                stackQ += "queueWord" + "__" + "%%NULL%%" + "__" + "queuePoS" + "__" + "%%NULL%%" + "__";
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
     * @return
     */
    String getTreeFeatures(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int sum, int indexOfwordInsen,
                           HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS
    ) {
        String stackT = "";
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
        for(int i=0; i<sentence.get(indexOfwordInsen).sons.size(); i++) {
            if(sumofchild >= sum)
                break;
            int index = sentence.get(indexOfwordInsen).sons.get(i);
            if(index < indexOfwordInsen) {
                indexofchild.set(sumofchild, index);
                sumofchild++;
            }
        }
        for(int i=0; i<sumofchild; i++) {
            Word w = _sentence.get(indexofchild.get(i));
            Stemmer s = new Stemmer();
            String str = w.word.toLowerCase();
            s.add(str.toCharArray(), str.length());
            s.stem();
            stackT += "treeQLeftWord" + "__" + s.toString() + "__" + "treeQLeftPoS" + "__" + w.PoS + "__";
        }
        // complement the blank
        if(sumofchild < sum) {
            for(int i=0; i<sum-sumofchild; i++) {
                stackT += "treeQLeftWord" + "__" + "%%NULL%%" + "__" + "treeQLeftPoS" + "__" + "%%NULL%%" + "__";
            }
        }
        //  Right
        indexofchild = new ArrayList<Integer>();
        sumofchild = 0;
        for(int i=0; i<sum; i++)
            indexofchild.add(-1);
        for(int i=sentence.get(indexOfwordInsen).sons.size()-1; i>=0; i--) {
            if(sumofchild >= sum)
                break;
            int index = sentence.get(indexOfwordInsen).sons.get(i);
            if(index > indexOfwordInsen) {
                indexofchild.set(sumofchild, index);
                sumofchild++;
            }
        }
        for(int i=0; i<sumofchild; i++) {
            Word w = _sentence.get(indexofchild.get(i));
            Stemmer s = new Stemmer();
            String str = w.word.toLowerCase();
            s.add(str.toCharArray(), str.length());
            s.stem();
            stackT += "treeQRightWord" + "__" + s.toString() + "__" + "treeQRightPoS" + "__" + w.PoS + "__";
        }
        // complement the blank
        if(sumofchild < sum) {
            for(int i=0; i<sum-sumofchild; i++) {
                stackT += "treeQRightWord" + "__" + "%%NULL%%" + "__" + "treeQRightPoS" + "__" + "%%NULL%%" + "__";
            }
        }
        // For Stack:
        if(!stack.isEmpty()) {
            // Left
            indexofchild = new ArrayList<Integer>();
            sumofchild = 0;
            Word w = stack.peek();
            for (int i = 0; i < sum; i++)
                indexofchild.add(-1);
            for (int i = 0; i < w.sons.size(); i++) {
                if (sumofchild >= sum)
                    break;
                int index = w.sons.get(i);
                if(index < indexOfwordInsen) {
                    indexofchild.set(sumofchild, index);
                    sumofchild++;
                }
            }
            for (int i = 0; i < sumofchild; i++) {
                w = _sentence.get(indexofchild.get(i));
                Stemmer s = new Stemmer();
                String str = w.word.toLowerCase();
                s.add(str.toCharArray(), str.length());
                s.stem();
                stackT += "treeSLeftWord" + "__" + s.toString() + "__" + "treeSLeftPoS" + "__" + w.PoS + "__";
            }
            // complement the blank
            if(sumofchild < sum) {
                for(int i=0; i<sum-sumofchild; i++) {
                    stackT += "treeSLeftWord" + "__" + "%%NULL%%" + "__" + "treeSLeftPoS" + "__" + "%%NULL%%" + "__";
                }
            }
            // Right
            indexofchild = new ArrayList<Integer>();
            sumofchild = 0;
            for (int i = 0; i < sum; i++)
                indexofchild.add(-1);
            for (int i = w.sons.size()-1; i>=0 ; i--) {
                if (sumofchild >= sum)
                    break;
                int index = w.sons.get(i);
                if(index > indexOfwordInsen) {
                    indexofchild.set(sumofchild, index);
                    sumofchild++;
                }
            }
            for (int i = 0; i < sumofchild; i++) {
                w = _sentence.get(indexofchild.get(i));
                Stemmer s = new Stemmer();
                String str = w.word.toLowerCase();
                s.add(str.toCharArray(), str.length());
                s.stem();
                stackT += "treeSLeftWord" + "__" + s.toString() + "__" + "treeSLeftPoS" + "__" + w.PoS + "__";
            }
            // complement the blank
            if(sumofchild < sum) {
                for(int i=0; i<sum-sumofchild; i++) {
                    stackT += "treeSLeftWord" + "__" + "%%NULL%%" + "__" + "treeSLeftPoS" + "__" + "%%NULL%%" + "__";
                }
            }
        }
        else {
            for(int i=0; i<sum*2; i++) {
                stackT += "treeSWord" + "__" + "%%NULL%%" + "__" + "treeSPoS" + "__" + "%%NULL%%" + "__";
            }
        }

        return stackT;
    }


    /**
     *
     * @param stack
     * @param sentence
     * @param _sentence
     * @param indexOfwordInsen
     * @param indexOfword
     * @param indexOfPoS
     * @return
     */
    String getDistanceFeatures(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                               HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackD = "";
        if(stack.empty())
            return "disSS__%%NULL%%__";
        stackD = String.valueOf(
                Math.abs(stack.peek().index - sentence.get(indexOfwordInsen).index)
        );
        return "disSS__" + stackD + "__";
    }

    String getSonFeatures(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                          HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackS = "";


        // Number of sons of stack word
        int sumL = 0, sumR = 0;
        if(stack.empty())
            stackS += "sonsLStack__%%NULL%%__sonsRStack__%%NULL%%__";
        else {
            Word st = stack.peek();
            for (int i = 0; i < st.sons.size(); i++) {
                if (st.sons.get(i) < st.index)
                    sumL++;
                else
                    sumR++;
            }
            stackS += "sonsLStack__" + String.valueOf(sumL) + "__sonsRStack__" + String.valueOf(sumR) + "__";
        }
        // Number of sons of sentence word
        Word sen = sentence.get(indexOfwordInsen);
        sumL = 0;
        sumR = 0;
        for(int i=0; i<sen.sons.size(); i++) {
            if(sen.sons.get(i) < sen.index)
                sumL ++;
            else
                sumR ++;
        }
        stackS += "sonsLSen__" + String.valueOf(sumL) + "__sonsRSen__" + String.valueOf(sumR) + "__";

        return stackS;
    }
}