package cn.edu.pku.parser.util;

import java.util.ArrayList;

public class Gen {
    public ArrayList<String> ChunkGen(ArrayList<String> Chunklabels, String chunklabel){
        ArrayList<String> al = new ArrayList<String>();

        for(int i=0; i<Chunklabels.size(); i++) {
            if(Chunklabels.get(i).equals("O")) {
                al.add("O");
            }
            else {
                String []str = Chunklabels.get(i).split("-");
                if(str[0].equals("I")) {
                    if(!chunklabel.equals("O") && !chunklabel.equals("")
                            && chunklabel.substring(2, chunklabel.length()).equals(str[1]))
                        al.add(Chunklabels.get(i));
                }
                else {
                    al.add(Chunklabels.get(i));
                }
            }
        }
        return al;
    }
}