package cn.edu.pku.parser.util;


import java.util.ArrayList;

/**
 * @author Siyuan Liu
 *
 * @function Information of one word
 */

public class Word {
    public String word = "";
    public String PoS = "";
    public int head = -1;
    public int index = -1;
    public ArrayList<Integer> sons = new ArrayList<Integer>();
    public Word(int index, String word, String PoS, int head) {
        this.index = index;
        this.word = word;
        this.PoS = PoS;
        this.head = head;
    }
}