package cn.edu.pku.parser.train;


import cn.edu.pku.parser.classifer.Perceptron;
import cn.edu.pku.parser.util.ExtractFeature;
import cn.edu.pku.parser.util.ExtractFeatureStr;
import cn.edu.pku.parser.util.Word;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class WordTrain {
    // sum of features
    int sumOffeatures = 10;
    // words information for training
    ArrayList<Word> words = new ArrayList<Word>();
    // sentence information for training
    ArrayList<ArrayList<Word>> sentences = new ArrayList<ArrayList<Word>>();
    // actions: 1-shift, 1-left, 2-right
    double SHIFT = 0;
    double LEFT = 1;
    double RIGHT = 2;

    // Words and PoSs indexing
    HashMap<String, Integer> indexOfword = new HashMap<String, Integer>();

    // Features and Actions for standard classifier
    Machine machine = new Machine();


    public WordTrain(String flname) throws Exception {
        indexOfword.put("%%NULL%%", 1);
        indexOfword.put("%%ROOT%%", 2);

        // Read training data and store them in words after transferring
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(flname), "utf-8"));
        String rline = null;
        ArrayList<Word> sentence = new ArrayList<Word>();
        sentence.add(new Word(0, "%%ROOT%%", "%%ROOT%%", -1));
        while( (rline = br.readLine()) != null) {
            String []elems = rline.split("\t");
            if(elems.length == 1) {// Just a blank line
                words.add(null);
                sentences.add(sentence);

                sentence = new ArrayList<Word>();
                sentence.add(new Word(0, "%%ROOT%%", "%%ROOT%%", -1));
            }
            else {
                Word w = new Word(
                        Integer.parseInt(elems[0]),
                        elems[1],
                        elems[3],
                        Integer.parseInt(elems[8]));
                words.add(w);
                sentence.add(w);
                // Build the words and PoSs indexing system
/*改成了全部小写*/
                if(!indexOfword.containsKey(elems[1].toLowerCase()))
                    indexOfword.put(elems[1].toLowerCase(), indexOfword.size()+1);
                if(!indexOfword.containsKey(elems[3]))
                    indexOfword.put(elems[3], indexOfword.size()+1);
            }
        }

        for(int i=0; i<sentences.size(); i++) {
            for(int j=1; j<sentences.get(i).size(); j++) {
                sentences.get(i).get(sentences.get(i).get(j).head).sons.add(j);
            }
        }
    }


    /* Form features and gold actions*/
    void formFeatures() {
        // For each step in SHIFT-REDUCE process
        //  Define feature templates and form entities
        //  Get gold action at current stage
        //NOTE: after forming the features, they are just some vectors facing the classifier.



        ArrayList<Feature[]> trainFeats = new ArrayList<Feature[]>();
        ArrayList<Double> actions = new ArrayList<Double>();
        // From with a sentence unit
        // Process of SHIFT-LEFT-RIGHT
        for(int i=0; i<sentences.size(); i++) {
            if(sentences.get(i).size()==1)
                continue;
            ArrayList<Word> _sentence = new ArrayList<Word>();
            for(int j=0; j<sentences.get(i).size(); j++) {
                Word w = sentences.get(i).get(j);
// -2 for default
                _sentence.add(new Word(w.index, w.word, w.PoS, -2));
            }
            ArrayList<Word> __sentence = new ArrayList<Word>(_sentence);
            Stack<Word> stack = new Stack<Word>();
            //Queue<Word> queue = new LinkedBlockingQueue(sentences.get(i));
            int indexOfwordInsen = 0;
            // step by step

            while(indexOfwordInsen < sentences.get(i).size()) { // either of them is not empty
/* 和PPT不符的部分 */
                // Form the features at this stage
                //  Transmit stack, queue, indexOfqueue into a machine of extracting features
                Feature[] fts = (new ExtractFeature()).features(stack, _sentence, __sentence, indexOfwordInsen, sumOffeatures,
                        indexOfword);
                // Get gold action
                double act = getGoldaction(stack, _sentence, indexOfwordInsen, sentences.get(i));
                //  Move the current stage with the gold action
                if(act == SHIFT) {
                    Word w = _sentence.get(indexOfwordInsen);
                    indexOfwordInsen ++;
                    stack.push(w);
                }
                if(act == LEFT) {
                    Word wStack = stack.pop();
                    wStack.head = _sentence.get(indexOfwordInsen).index;
                    _sentence.get(indexOfwordInsen).sons.add(wStack.index);
/* 检查sentences里面词汇head是否同时被改变 */
                }
                if(act == RIGHT) {
                    Word wStack = stack.pop();
                    _sentence.get(indexOfwordInsen).head = wStack.index;
                    wStack.sons.add(_sentence.get(indexOfwordInsen).index);

                    _sentence.set(indexOfwordInsen, wStack);
                }

                // Store the information of current stage for forming features
                trainFeats.add(fts);
                actions.add(act);
            }
        }

        // Form the machine
        machine.problem.l = trainFeats.size();
        machine.problem.n = 28*indexOfword.size();
/* 这可能有错误 */
        machine.problem.x = new Feature[trainFeats.size()][];
        machine.problem.y = new double[trainFeats.size()];

        for(int i=0; i<trainFeats.size(); i++) {
            machine.problem.x[i] = trainFeats.get(i);
            machine.problem.y[i] = actions.get(i);
        }
        //machine.problem.bias = -1;

    }


    /* Put into classifier and get a model*/
    public void train() throws IOException {
        // formFeatures
        formFeatures();

        // Put into classifier
        Model model = Linear.train(machine.problem, machine.parameter);

        // Test
        test(model);
    }


    double getGoldaction(Stack<Word> stack, ArrayList<Word> sentence, int indexOfwordInsen, ArrayList<Word> goldsentence) {
        //ArrayList<String> validActions = getValidActions(indexOfwordInsen, stack, sentence);
        if(!stack.isEmpty()) {
            Word wStack = stack.peek();
            Word wSen = sentence.get(indexOfwordInsen);
            Word _wStack = goldsentence.get(wStack.index);
            Word _wSen = goldsentence.get(wSen.index);
            if (_wStack.head == _wSen.index && isCompleted(_wStack, wStack))
                return LEFT;
            else if (_wSen.head == _wStack.index && isCompleted(_wSen, wSen))
                return RIGHT;
        }
        return SHIFT;
    }

    /**
     *
     * @param index: index of the word operated in current sentence
     * @param stack
     * @param sentence
     * @return
     */
    ArrayList<Double> getValidActions(int index, Stack<Word> stack, ArrayList<Word> sentence) {
        ArrayList<Double> valid = new ArrayList<Double>();
        if(index < sentence.size()) {
            if(index < sentence.size()-1 ||
                    stack.size() == 0)
                valid.add(SHIFT);

            if(stack.size() >= 1)
                valid.add(RIGHT);

            if(stack.size() >= 2)
                valid.add(LEFT);
        }
        return valid;
    }

    boolean isCompleted(Word wGold, Word myWord) {
        for(int i=0; i<wGold.sons.size(); i++) {
            if(!myWord.sons.contains(wGold.sons.get(i)))
                return false;
        }
        return true;
    }




    void test(Model model) throws IOException {
        ArrayList<ArrayList<Word>> sentences = new ArrayList<ArrayList<Word>>();

        // Read training data and store them in words after transferring
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("./data/dev.conll08"), "utf-8"));
        String rline = null;
        ArrayList<Word> sentence = new ArrayList<Word>();
        sentence.add(new Word(0, "%%ROOT%%", "%%ROOT%%", -1));
        while( (rline = br.readLine()) != null) {
            String []elems = rline.split("\t");
            if(elems.length == 1) {// Just a blank line
                words.add(null);
                sentences.add(sentence);

                sentence = new ArrayList<Word>();
                sentence.add(new Word(0, "%%ROOT%%", "%%ROOT%%", -1));
            }
            else {
                Word w = new Word(
                        Integer.parseInt(elems[0]),
                        elems[1],
                        elems[3],
                        Integer.parseInt(elems[8])
                        //0
                );

                words.add(w);
                sentence.add(w);
            }
        }

        for(int i=0; i<sentences.size(); i++) {
            for(int j=1; j<sentences.get(i).size(); j++) {
                sentences.get(i).get(sentences.get(i).get(j).head).sons.add(j);
            }
        }

        // =====
        // From with a sentence unit
        // Process of SHIFT-LEFT-RIGHT
        int right =0;
        int sum = 0;
        FileWriter fileWriter = new FileWriter("./data/dev.re");
        for (int i = 0; i < sentences.size(); i++) {
            if (sentences.get(i).size() == 1)
                continue;
            ArrayList<Word> _sentence = new ArrayList<Word>();
            for (int j = 0; j < sentences.get(i).size(); j++) {
                Word w = sentences.get(i).get(j);
// -2 for default
                _sentence.add(new Word(w.index, w.word, w.PoS, -2));
            }
            ArrayList<Word> __sentence = new ArrayList<Word>(_sentence);
            Stack<Word> stack = new Stack<Word>();
            //Queue<Word> queue = new LinkedBlockingQueue(sentences.get(i));
            int indexOfwordInsen = 0;
            // step by step

            while (indexOfwordInsen < sentences.get(i).size()) { // either of them is not empty
/* 和PPT不符的部分 */
                // Form the features at this stage
                //  Transmit stack, queue, indexOfqueue into a machine of extracting features
                Feature[] fts = (new ExtractFeature()).features(stack, _sentence, __sentence, indexOfwordInsen, sumOffeatures,
                        indexOfword);
                // Get gold action
                double []d = new double[3];
                double act = 0.0;
                Linear.predictValues(model, fts, d);
                ArrayList<Double> valid = getValidActions(indexOfwordInsen, stack, _sentence);
                double tmpW = -1;
                double actions[] = {0.0, 1.0, 2.0};
                for(int v=0; v<3; v++) {
                    if(valid.contains(actions[v]) && d[v] > tmpW) {
                        act = actions[v];
                        tmpW = d[v];
                    }
                }

                //  Move the current stage with the gold action
                if (String.valueOf(act).equals("0.0")) {
                    Word w = _sentence.get(indexOfwordInsen);
                    indexOfwordInsen++;
                    stack.push(w);
                }
                else if (String.valueOf(act).equals("1.0")) {
                    Word wStack = stack.pop();
                    wStack.head = _sentence.get(indexOfwordInsen).index;
                    _sentence.get(indexOfwordInsen).sons.add(wStack.index);
/* 检查sentences里面词汇head是否同时被改变 */
                }
                else if (String.valueOf(act).equals("2.0")) {
                    Word wStack = stack.pop();
                    _sentence.get(indexOfwordInsen).head = wStack.index;
                    wStack.sons.add(_sentence.get(indexOfwordInsen).index);

                    _sentence.set(indexOfwordInsen, wStack);
                }
                else
                    System.out.println(act);
            }
            sum += __sentence.size();
            for(int ii=0; ii<__sentence.size(); ii++) {
                if(ii != 0) {
                    Word w = __sentence.get(ii);
                    fileWriter.write(String.valueOf(w.index) + "\t" + w.word + "\t" + w.word + "\t" + w.PoS + "\t" + w.PoS
                            + "\t_\t_\t_\t" + String.valueOf(w.head) + "\tX\t");
                    //for(int jj = 0; jj<100; jj++)
                    //    fileWriter.write("_\t");
                    fileWriter.write("_\n");

                }
                if(__sentence.get(ii).head == sentences.get(i).get(ii).head)
                    right ++;
            }
            fileWriter.write("\n");
        }
        fileWriter.close();
        System.out.println((double)right/sum);
    }

}