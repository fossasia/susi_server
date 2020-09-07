/**
 *  SusiSkillFile
 *  Copyright 05.07.2019 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ai.susi.DAO;
import ai.susi.SusiServer;

public class SusiSkillFile implements Iterator<SusiSkillFile.IntentBlock>, Iterable<SusiSkillFile.IntentBlock> {

    private final ArrayList<IntentBlock> intents;
    private int pos;

    public SusiSkillFile() throws IOException {
        this.intents = new ArrayList<>();
        this.pos = 0;
    }
    
    public SusiSkillFile(final BufferedReader br) throws IOException {
    	this();
        List<List<Line>> blocks = textBlockReader(br);
        blocks.forEach(block -> intents.add(new IntentBlock(block)));
    }

    public static SusiSkillFile load(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        SusiSkillFile skillfile = new SusiSkillFile(br);
        br.close();
        return skillfile;
    }

    public void save(File f) throws IOException {
        FileWriter fw = new FileWriter(f);
        fw.write(this.toString());
        fw.close();
    }

    public static class IntentBlock {

        public final String utterance;
        public final List<Line> model;
        public int lineNumber;

        public IntentBlock(final List<Line> numberedLines) {
            List<Line> a = new ArrayList<>();

            // first merge lines with line-glue "\\" into one line
            for (int i = 0; i < numberedLines.size(); i++) {
                Line numberedLine = numberedLines.get(i);
                assert numberedLine.line.length() > 0;
                if (numberedLine.line.charAt(0) == '\\') {
                    if (a.size() == 0) {
                        a.add(new Line(numberedLine.line.substring(1), numberedLine.number));
                    } else {
                        Line prevLine = a.get(a.size() - 1);
                        a.set(a.size() - 1, new Line(prevLine.line + ' ' + numberedLine.line.substring(1).trim(), prevLine.number));
                    }
                } else {
                    if (a.size() == 0) {
                        a.add(numberedLine);
                    } else {
                        Line prevLine = a.get(a.size() - 1);
                        String lasta = prevLine.line;
                        if (lasta.charAt(lasta.length() - 1) == '\\') {
                            a.set(a.size() - 1, new Line(lasta.substring(0, lasta.length() - 1) + ' ' + numberedLine.line.trim(), prevLine.number));
                        } else {
                            a.add(numberedLine);
                        }
                    }
                }
            };

            // separate head from utterance and model
            this.model = a.size() < 2 ? null : new ArrayList<>();
            String impl = null;
            for (int i = 0; i < a.size(); i++) {
                Line line = a.get(i);
                if (impl == null) {
                    impl = line.line;
                    this.lineNumber = line.number;
                } else {
                    model.add(line);
                }
            };
            this.utterance = impl;
        }

        public String getUtteranceFingerprint() {
            String l = this.utterance.toLowerCase();
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < l.length(); i++) {
                char c = l.charAt(i);
                if (c >= 'a' && c <= 'z') s.append(c);
                if (s.length() > 21) break;
            }
            while (s.length() <= 21) s.append('x');
            StringBuilder h = new StringBuilder(8);
            h.append(s.charAt(0)); h.append(s.charAt(1)); h.append(s.charAt(2)); h.append(s.charAt(3));
            h.append(s.charAt(5)); h.append(s.charAt(8)); h.append(s.charAt(13)); h.append(s.charAt(21));
            return h.toString();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(utterance).append("\n");
            if (this.model != null) {
                for (Line p: model) {
                    sb.append(p.line).append("\n");
                }
            }
            sb.append("\n");
            return sb.toString();
        }
    }

    public final static Pattern tabPattern = Pattern.compile("\t");

    public final static class Line {
        public String line;
        public int number;
        public Line(String line, int number) {
            this.line = line;
            this.number = number;
        }
    }
    
    /**
     * Read bags of lines:
     * Each bag is a set of lines which have no empty lines between them.
     * Inside the bag there are lines of the section.
     * @param br
     * @return a list of line bags, with lines inside
     * @throws IOException
     */
    public static List<List<Line>> textBlockReader(final BufferedReader br) throws IOException {
        String line = "";
        List<Line> line_bag = new ArrayList<>();
        List<List<Line>> blocks = new ArrayList<>();
        int lineNumber = -1;
        while ((line = br.readLine()) != null) {
            lineNumber++;

            line = tabPattern.matcher(righttrim(line)).replaceAll("    ");

            // empty lines close a block
            if (line.length() == 0) {
                if (line_bag.size() > 0) {
                    blocks.add(line_bag);
                    line_bag = new ArrayList<>();
                }
                continue;
            }
            // lines containing "::" at the beginning are always single lines
            if (line.startsWith("::")) {
                if (line_bag.size() > 0) {
                    blocks.add(line_bag);
                    line_bag = new ArrayList<>();
                }
                line_bag.add(new Line(line, lineNumber));
                blocks.add(line_bag);
                line_bag = new ArrayList<>();
                continue;
            }

            // comment lines are invisible, as they are not there
            if (line.charAt(0) == '#') continue;

            // all other lines are added to a block
            line_bag.add(new Line(line, lineNumber));
        }
        // there might be a unsaved block at the end
        if (line_bag.size() > 0) {
            blocks.add(line_bag);
        }
        return blocks;
    }

    public static String righttrim(String s) {
        int len = s.length();
        while (0 < len && s.charAt(len - 1) <= ' ') len--;
        return len < s.length() ? s.substring(0, len) : s;
    }

    public ArrayList<IntentBlock> getIntents() {
        return this.intents;
    }

    public IntentBlock getIntent(String utterance) {
        for (IntentBlock intent: intents) {
            if (utterance.equals(intent.utterance)) return intent;
        }
        return null;
    }

    public void setIntent(IntentBlock intentNew) {
        for (IntentBlock intent: intents) {
            if (intentNew.utterance.equals(intent.utterance)) {
                intent.model.clear();
                intent.model.addAll(intentNew.model);
                return;
            }
        }
        this.intents.add(intentNew);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IntentBlock intent: intents) {
            sb.append(intent.toString());
        }
        return sb.toString();
    }

    @Override
    public Iterator<IntentBlock> iterator() {
        return this.intents.iterator();
    }

    @Override
    public boolean hasNext() {
        return pos < this.intents.size();
    }

    @Override
    public IntentBlock next() {
        return this.intents.get(this.pos++);
    }

    public static void main(String[] args) {
        Path data = FileSystems.getDefault().getPath("data");

        try {
            Map<String, String> config = SusiServer.readConfig(data);
            DAO.init(config, data, true);
            File f = new File("conf/os_skills/operation/en/en_0001_foundation.txt");
            SusiSkill.ID skillid = new SusiSkill.ID(f);
            
            String test =
                    "pause | pause that | pause this | pause *\n" + 
                    "!console:\n" + 
                    "{\"actions\":[{\"type\":\"pause\"}]}\n" + 
                    "eol";

            SusiSkillFile ssf = new SusiSkillFile(new BufferedReader(new StringReader(test)));
            System.out.println(ssf.toString());
            SusiSkill ss = new SusiSkill(ssf, skillid, false);
            System.out.println(ss.toJSON().toString(2));

            ssf = SusiSkillFile.load(f);
            System.out.println(ssf.toString());
            ss = new SusiSkill(ssf, skillid, false);
            System.out.println(ss.toJSON().toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
