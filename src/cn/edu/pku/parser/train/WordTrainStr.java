package cn.edu.pku.parser.train;


import cn.edu.pku.parser.classifer.Perceptron;
import cn.edu.pku.parser.util.*;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class WordTrainStr {
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
    HashMap<String, Integer> indexOfPoS = new HashMap<String, Integer>();

    // Features and Actions for standard classifier
    Machine machine = new Machine();

    // Size of Beam
    int beamsize = 5;
    public WordTrainStr(String flname) throws Exception {
        indexOfword.put("%%NULL%%", 0);
        indexOfPoS.put("%%NULL%%", 0);
        indexOfword.put("%%ROOT%%", 1);
        indexOfPoS.put("%%ROOT%%", 1);

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
                Stemmer s = new Stemmer();
                String str = elems[1].toLowerCase();
                s.add(str.toCharArray(), str.length());
                s.stem();
                if(!indexOfword.containsKey(elems[1].toLowerCase()))
                    indexOfword.put(s.toString(), indexOfword.size());
                if(!indexOfPoS.containsKey(elems[3]))
                    indexOfPoS.put(elems[3], indexOfPoS.size());
            }
        }

        for(int i=0; i<sentences.size(); i++) {
            for(int j=1; j<sentences.get(i).size(); j++) {
                sentences.get(i).get(sentences.get(i).get(j).head).sons.add(j);
            }
        }
    }





    /* Put into classifier and get a model*/
    public void train() throws IOException {
        // formFeatures
        //formFeatures();
        Perceptron p = formFeaturesStr();
        test(p);
        // Put into classifier
        //Model model = Linear.train(machine.problem, machine.parameter);

    }


    double getGoldaction(Stack<Word> stack, ArrayList<Word> sentence, int indexOfwordInsen, ArrayList<Word> goldsentence) {
        ArrayList<String> validActions = getValidActions(indexOfwordInsen, stack, sentence);
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
    ArrayList<String> getValidActions(int index, Stack<Word> stack, ArrayList<Word> sentence) {
        ArrayList<String> valid = new ArrayList<String>();
        if(index < sentence.size()) {
            if(index < sentence.size()-1 ||
                    stack.size() == 0)
                valid.add("0.0");

            if(stack.size() >= 1)
                valid.add("2.0");

            if(stack.size() >= 2)
                valid.add("1.0");
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



   Perceptron formFeaturesStr() {
   // For each step in SHIFT-REDUCE process
        //  Define feature templates and form entities
        //  Get gold action at current stage
        //NOTE: after forming the features, they are just some vectors facing the classifier.


        ArrayList<String> trainFeats = new ArrayList<String>();
        ArrayList<Double> actions = new ArrayList<Double>();
        // From with a sentence unit
        // Process of SHIFT-LEFT-RIGHT
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
                String fts = (new ExtractFeatureStr()).features(stack, _sentence, __sentence, indexOfwordInsen, sumOffeatures,
                        indexOfword, indexOfPoS);
                // Get gold action
                double act = getGoldaction(stack, _sentence, indexOfwordInsen, sentences.get(i));
                //  Move the current stage with the gold action
                if (act == SHIFT) {
                    Word w = _sentence.get(indexOfwordInsen);
                    indexOfwordInsen++;
                    stack.push(w);
                }
                if (act == LEFT) {
                    Word wStack = stack.pop();
                    wStack.head = _sentence.get(indexOfwordInsen).index;
                    _sentence.get(indexOfwordInsen).sons.add(wStack.index);
/* 检查sentences里面词汇head是否同时被改变 */
                }
                if (act == RIGHT) {
                    Word wStack = stack.pop();
                    _sentence.get(indexOfwordInsen).head = wStack.index;
                    wStack.sons.add(_sentence.get(indexOfwordInsen).index);

                    _sentence.set(indexOfwordInsen, wStack);
                }

                // Store the information of current stage for forming features
                trainFeats.add(fts + "label__" + String.valueOf(act));
                actions.add(act);
            }
        }
        ArrayList<String> labels = new ArrayList<String>();
        labels.add("0.0");
        labels.add("1.0");
        labels.add("2.0");
        Perceptron p = new Perceptron(67, labels, new Gen());
        p.trainMachine(trainFeats, 0.006);
        System.out.println();
        return p;
    }

    void test(Perceptron p) throws IOException {
        ArrayList<ArrayList<Word>> sentences = new ArrayList<ArrayList<Word>>();

        // Read training data and store them in words after transferring
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("./data/test.conll08"), "utf-8"));
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
                        //<<<<DEV
                        //Integer.parseInt(elems[8])
                        0);

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
        FileWriter fileWriter = new FileWriter("./data/test.re");
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
                String fts = (new ExtractFeatureStr()).features(stack, _sentence, __sentence, indexOfwordInsen, sumOffeatures,
                        indexOfword, indexOfPoS);
                // Get gold action
                ArrayList<String> valid = getValidActions(indexOfwordInsen, stack, _sentence);
                String act = p.labelofInstance(fts, valid);
                //  Move the current stage with the gold action
                if (act == "0.0") {
                    Word w = _sentence.get(indexOfwordInsen);
                    indexOfwordInsen++;
                    stack.push(w);
                }
                if (act == "1.0") {
                    Word wStack = stack.pop();
                    wStack.head = _sentence.get(indexOfwordInsen).index;
                    _sentence.get(indexOfwordInsen).sons.add(wStack.index);
/* 检查sentences里面词汇head是否同时被改变 */
                }
                if (act == "2.0") {
                    Word wStack = stack.pop();
                    _sentence.get(indexOfwordInsen).head = wStack.index;
                    wStack.sons.add(_sentence.get(indexOfwordInsen).index);

                    _sentence.set(indexOfwordInsen, wStack);
                }
            }
            sum += __sentence.size();
            for(int ii=0; ii<__sentence.size(); ii++) {
                if(ii != 0) {
                    Word w = __sentence.get(ii);
                    fileWriter.write(String.valueOf(w.index) + "\t" + w.word + "\t" + w.word + "\t" + w.PoS + "\t" + w.PoS
                            + "\t_\t_\t_\t" + String.valueOf(w.head) + "\tX\t");

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


    void testBeam(Perceptron p) throws IOException {
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
                        //<<<<DEV
                        Integer.parseInt(elems[8])
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
            ArrayList<Beam> beam = new ArrayList<Beam>();
            beam.add(new Beam(sentences.get(i)));

            //Queue<Word> queue = new LinkedBlockingQueue(sentences.get(i));

            for(int ac=0; ac<sentences.get(i).size()*2-1; ac++) {
                ArrayList<Beam> tmp_beam = new ArrayList<Beam>();

                for(int be=0; be<beam.size(); be++) {
                    Beam b = beam.get(be);
                    String fts = (new ExtractFeatureStr()).features(b.stack, b._sentence, b.__sentence, b.indexOfwordInsen, sumOffeatures,
                            indexOfword, indexOfPoS);
                    // Get gold action
                    ArrayList<String> valid = getValidActions(b.indexOfwordInsen, b.stack, b._sentence);
                    double []score = p.labelScores(fts);

                    valid.size();
                    for(int va=0; va<valid.size(); va++) {
                        // take action
                        Beam newB = new Beam(b._sentence, b.__sentence, b.stack, b.indexOfwordInsen, b.score);

                        if (valid.get(va) == "0.0") {
                            Word w = newB._sentence.get(newB.indexOfwordInsen);
                            newB.indexOfwordInsen++;
                            newB.stack.push(w);
                            newB.score += score[0];
                        }
                        if (valid.get(va) == "1.0") {
                            Word wStack = newB.stack.pop();
                            wStack.head = newB._sentence.get(newB.indexOfwordInsen).index;
                            newB._sentence.get(newB.indexOfwordInsen).sons.add(wStack.index);
                            newB.score += score[1];
/* 检查sentences里面词汇head是否同时被改变 */
                        }
                        if (valid.get(va) == "2.0") {
                            Word wStack = newB.stack.pop();
                            newB._sentence.get(newB.indexOfwordInsen).head = wStack.index;
                            wStack.sons.add(newB._sentence.get(newB.indexOfwordInsen).index);

                            newB._sentence.set(newB.indexOfwordInsen, wStack);
                            newB.score += score[2];
                        }
                        tmp_beam.add(newB);
                    }
                }

                // Sort the tmp_beam
                Collections.sort(tmp_beam, new Comparator<Beam>() {
                    public int compare(Beam s1, Beam s2) {
                        return s2.score - s1.score;
                    }
                });
                beam.clear();

                beam = new ArrayList<Beam>();
                for(int t=0; t<tmp_beam.size(); t++) {
                    if(beam.size() == beamsize)
                        break;
                    beam.add(tmp_beam.get(t));
                }
                beam.size();
            }


            Beam b = beam.get(0);
            sum += b.__sentence.size();
            for(int ii=0; ii<b.__sentence.size(); ii++) {
                if(ii != 0) {
                    Word w = b.__sentence.get(ii);
                    fileWriter.write(String.valueOf(w.index) + "\t" + w.word + "\t" + w.word + "\t" + w.PoS + "\t" + w.PoS
                            + "\t_\t_\t_\t" + String.valueOf(w.head) + "\tX\t");

                    fileWriter.write("_\n");

                }
                if(b.__sentence.get(ii).head == sentences.get(i).get(ii).head)
                    right ++;
            }
            fileWriter.write("\n");

        }
        fileWriter.close();
        System.out.println((double)right/sum);
    }

    class Beam {
        ArrayList<Word> _sentence = new ArrayList<Word>();
        ArrayList<Word> __sentence;
        Stack<Word> stack = new Stack<Word>();
        int indexOfwordInsen = 0;
        int score = 0;

        public Beam(ArrayList<Word> sentence) {

            for (int j = 0; j < sentence.size(); j++) {
                Word w = sentence.get(j);
                // -2 for default
                _sentence.add(new Word(w.index, w.word, w.PoS, -2));
            }
            __sentence = new ArrayList<Word>(_sentence);
        }
        public Beam(ArrayList<Word> _sentence, ArrayList<Word> __sentence, Stack<Word> stack, int indexOfwordInsen, int score) {
            this._sentence = (ArrayList<Word>)_sentence.clone();
            this.__sentence = (ArrayList<Word>)__sentence.clone();
            this.stack = (Stack<Word>)stack.clone();
            this.indexOfwordInsen = indexOfwordInsen;
            this.score = score;
        }
    }
}