package cn.edu.pku.parser.util;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class ExtractFeatureStr {
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
        String stackF = getStackFeatures(stack, 1, indexOfword, indexOfPoS);//1-2
        f = stackF;

        /* Feature from queue in sentence */
        String stackQ = getQueueFeatures(sentence, 3, indexOfwordInsen, indexOfword, indexOfPoS);//1-2
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

        /* Feature from Single Combination */
        String stackSg = getSingleComb(stack, sentence, _sentence, indexOfwordInsen, indexOfword, indexOfPoS); //2
        f += stackSg;

        /* Feature from Pair Combination */
        String stackP = getPairComb(stack, sentence, _sentence, indexOfwordInsen, indexOfword, indexOfPoS); //7
        f += stackP;

        /* Feature from Pair Combination */
        String stackTr = getTriCom(stack, sentence, _sentence, indexOfwordInsen, indexOfword, indexOfPoS); //5
        f += stackTr;

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
            stackF += "stackWord" + "__" + w.word.toLowerCase() + "__" + "stackPoS" + "__" + w.PoS + "__";
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
            stackQ += "queueWord" + "__" + w.word.toLowerCase() + "__" + "queuePoS" + "__" + w.PoS + "__";
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
            stackT += "treeQLeftWord" + "__" + w.word.toLowerCase() + "__" + "treeQLeftPoS" + "__" + w.PoS + "__";
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
            stackT += "treeQRightWord" + "__" + w.word.toLowerCase() + "__" + "treeQRightPoS" + "__" + w.PoS + "__";
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
                stackT += "treeSLeftWord" + "__" + w.word.toLowerCase() + "__" + "treeSLeftPoS" + "__" + w.PoS + "__";
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
                stackT += "treeSLeftWord" + "__" + w.word.toLowerCase() + "__" + "treeSLeftPoS" + "__" + w.PoS + "__";
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

        if(stack.empty()) {

            // distance
            stackD += "disSS__%%NULL%%__";

            // S0wd
            stackD += "disSS__%%NULL%%__";
            // S0pd
            stackD += "disSS__%%NULL%%__";
            // N0wd
            stackD += "disSS__%%NULL%%__";
            // N0pd
            stackD += "disSS__%%NULL%%__";
            // S0wN0wd
            stackD += "disSS__%%NULL%%__";
            // S0pN0pd
            stackD += "disSS__%%NULL%%__";

            return stackD;
        }

        String dis = String.valueOf(
            Math.abs(stack.peek().index - sentence.get(indexOfwordInsen).index)
        );

        Word st = stack.peek();
        Word sen = sentence.get(indexOfwordInsen);

        // distance
        stackD += "disSS__" + dis + "__";
        // S0wd
        stackD += "disSS__" + st.word.toLowerCase() + "-" + dis +"__";
        // S0pd
        stackD += "disSS__" + st.PoS + "-" + dis +"__";
        // N0wd
        stackD += "disSS__" + sen.word.toLowerCase() + "-" + dis +"__";
        // N0pd
        stackD += "disSS__" + sen.PoS + "-" + dis +"__";
        // S0wN0wd
        stackD += "disSS__" + st.word.toLowerCase() + "-" + sen.word.toLowerCase() + "-" + dis +"__";
        // S0pN0pd
        stackD += "disSS__" + st.PoS + "-" + st.PoS + "-" + dis +"__";

        return stackD;
    }

    String getSonFeatures(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                          HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackS = "";


        // Number of sons of stack word
        int sumL = 0, sumR = 0;
        if(stack.empty()) {
            stackS += "sonsLStack__%%NULL%%__sonsRStack__%%NULL%%__";
            // Combinations
            stackS += "sons__%%NULL%%__";
            stackS += "sons__%%NULL%%__";
            stackS += "sons__%%NULL%%__";
            stackS += "sons__%%NULL%%__";
        }
        else {
            Word st = stack.peek();
            for (int i = 0; i < st.sons.size(); i++) {
                if (st.sons.get(i) < st.index)
                    sumL++;
                else
                    sumR++;
            }
            stackS += "sonsLStack__" + String.valueOf(sumL) + "__sonsRStack__" + String.valueOf(sumR) + "__";

            // Combinations
            stackS += "sons__" + st.word.toLowerCase() + "-" + String.valueOf(sumL) + "__";
            stackS += "sons__" + st.PoS + "-" + String.valueOf(sumL) + "__";
            stackS += "sons__" + st.word.toLowerCase() + "-" + String.valueOf(sumR) + "__";
            stackS += "sons__" + st.PoS + "-" + String.valueOf(sumR) + "__";
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
        // Combination
        stackS += "sons__" + sen.word.toLowerCase() + "-" + String.valueOf(sumL) + "__";
        stackS += "sons__" + sen.PoS + "-" + String.valueOf(sumL) + "__";
        stackS += "sons__" + sen.word.toLowerCase() + "-" + String.valueOf(sumR) + "__";
        stackS += "sons__" + sen.PoS + "-" + String.valueOf(sumR) + "__";

        return stackS;
    }

    String getSingleComb(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                         HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackSg = "";
        // Queue
        Word sen = sentence.get(indexOfwordInsen);
        stackSg += "queueWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "__";

        if(indexOfwordInsen < _sentence.size()-1) {
            sen = sentence.get(indexOfwordInsen+1);
            stackSg += "queueWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "__";
        }
        else
            stackSg += "queueWP__%%NULL$$__";
        if(indexOfwordInsen < _sentence.size()-2) {
            sen = sentence.get(indexOfwordInsen+2);
            stackSg += "queueWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "__";
        }
        else
            stackSg += "queueWP__%%NULL$$__";

        // Stack
        if(stack.empty())
            stackSg += "stackWP__%%NULL%%__";
        else {
            Word st = stack.peek();
            stackSg += "stackWP__" + st.word.toLowerCase() + "-" + st.PoS + "__";
        }

        return stackSg;
    }

    String getPairComb(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                       HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackP = "";

        Word sen = sentence.get(indexOfwordInsen);
        if(stack.empty()) {
            // S0wpN0wp
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + "%%NULL%%__";
            // S0wpN0w
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + "%%NULL%%__";
            // S0wN0wp
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + "%%NULL%%__";
            // S0wpN0p
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + "%%NULL%%__";
            // S0pN0wp
            stackP += "stackPWP__" + sen.PoS + "-" + "%%NULL%%__";
            // S0wN0w
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + "%%NULL%%__";
            // S0pN0p
            stackP += "stackPWP__" + sen.PoS + "-" + "%%NULL%%__";
        }
        else {
            Word st = stack.peek();
            // S0wpN0wp
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + st.word.toLowerCase() + "-" + st.PoS + "__";
            // S0wpN0w
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + st.word.toLowerCase() + "__";
            // S0wN0wp
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + st.word.toLowerCase() + "-" + st.PoS + "__";
            // S0wpN0p
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + sen.PoS + "-" + st.PoS + "__";
            // S0pN0wp
            stackP += "stackPWP__" + sen.PoS + "-" + st.word.toLowerCase() + "-" + st.PoS + "__";
            // S0wN0w
            stackP += "stackPWP__" + sen.word.toLowerCase() + "-" + st.word.toLowerCase() + "__";
            // S0pN0p
            stackP += "stackPWP__" + sen.PoS + "-" + st.PoS + "__";
        }

        return stackP;
    }

    String getTriCom(Stack<Word> stack, ArrayList<Word> sentence, ArrayList<Word> _sentence, int indexOfwordInsen,
                     HashMap<String, Integer> indexOfword, HashMap<String, Integer> indexOfPoS) {
        String stackTr = "";

        String N0, N1, N2, N0l;
        String S0, S0l, S0r;

        N0 = sentence.get(indexOfwordInsen).PoS;
        if(indexOfwordInsen < _sentence.size()-1)
            N1 = sentence.get(indexOfwordInsen+1).PoS;
        else
            N1 = "%%NULL%%";
        if(indexOfwordInsen < _sentence.size()-2)
            N2 = sentence.get(indexOfwordInsen+2).PoS;
        else
            N2 = "%%NULL%%";

        if(!sentence.get(indexOfwordInsen).sons.isEmpty()) {
            int idx = sentence.get(indexOfwordInsen).sons.get(0);
            if(idx < indexOfwordInsen)
                N0l = _sentence.get(idx).PoS;
            else
                N0l = "%%NULL%%";
        }
        else
            N0l = "%%NULL%%";

        if(!stack.empty()) {
            Word st = stack.peek();
            S0 = st.PoS;

            if(!st.sons.isEmpty()) {
                int idx = st.sons.get(0);
                if(idx < indexOfwordInsen) {
                    S0l = _sentence.get(idx).PoS;
                }
                else {
                    S0l = "%%NULL%%";
                }

                idx = st.sons.get(st.sons.size()-1);
                if(idx > indexOfwordInsen) {
                    S0r = _sentence.get(idx).PoS;
                }
                else {
                    S0r = "%%NULL%%";
                }
            }
            else {
                S0l = "%%NULL%%";
                S0r = "%%NULL%%";

            }
        }
        else {
            S0 = "%%NULL%%";
            S0l = "%%NULL%%";
            S0r = "%%NULL%%";
        }
        ////////////
        stackTr += "N0pN1pN2p__" + N0 + "-" + N1 + "-" + N2 + "__";
        stackTr += "S0pN0pN1p__" + S0 + "-" + N0 + "-" + N1 + "__";
        stackTr += "S0pS0lpN0p__" + S0 + "-" + S0l + "-" + N0 + "__";
        stackTr += "S0pS0rpN0p__" + S0 + "-" + S0r + "-" + N0 + "__";
        stackTr += "S0pN0pN0lp__" + S0 + "-" + N0 + "-" + N0l + "__";

        return stackTr;
    }
}