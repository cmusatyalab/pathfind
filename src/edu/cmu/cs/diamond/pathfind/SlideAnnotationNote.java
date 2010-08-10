/*
 *  PathFind -- a Diamond system for pathology
 *
 *  Copyright (c) 2008-2010 Carnegie Mellon University
 *  All rights reserved.
 *
 *  PathFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  PathFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PathFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking PathFind statically or dynamically with other modules is
 *  making a combined work based on PathFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  In addition, as a special exception, the copyright holders of
 *  PathFind give you permission to combine PathFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for PathFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of PathFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

package edu.cmu.cs.diamond.pathfind;
import org.antlr.stringtemplate.StringTemplate;

import edu.cmu.cs.diamond.pathfind.annotations.Note;


public class SlideAnnotationNote {
    private final String creator;

    public String getCreator() {
        return creator;
    }

    public Integer getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    private final Integer id;

    private final String text;

    public SlideAnnotationNote(String creator, Integer id, String text) {
        this.creator = creator;
        this.id = id;
        this.text = text;
    }

    public Note toJAXB() {
        Note n = new Note();

        n.setCreator(creator);
        n.setId(id);
        n.setText(text);

        return n;
    }

    public SlideAnnotationNote(Note n) {
        this(n.getCreator(), n.getId(), n.getText());
    }

    public SlideAnnotationNote(String text) {
        this(null, null, text);
    }

    public void populateStringTemplateAttributes(StringTemplate t) {
        t.setAttribute("creator", getCreator());
        t.setAttribute("text", getText());
    }

    @Override
    public String toString() {
        return "creator: " + creator + ", id: " + id + ", text: " + text;
    }
}
