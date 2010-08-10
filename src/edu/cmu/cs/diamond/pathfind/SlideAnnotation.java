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
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.cs.openslide.gui.Annotation;


class SlideAnnotation implements Annotation {
    final private List<SlideAnnotationNote> notes;

    final private String creator;

    public String getCreator() {
        return creator;
    }

    final private Integer id;

    final private Shape shape;

    final static private List<SlideAnnotationNote> EMPTY_LIST = Collections
            .emptyList();

    public SlideAnnotation(Shape shape, Integer id, String creator,
            List<SlideAnnotationNote> notes) {
        if (shape == null) {
            throw new NullPointerException("shape cannot be null");
        }
        this.shape = new ImmutableShape(shape);

        // deep copy for immutability
        this.notes = Collections
                .unmodifiableList(new ArrayList<SlideAnnotationNote>(notes));

        this.id = id;
        this.creator = creator;
    }

    public SlideAnnotation(Shape shape) {
        this(shape, null, null, EMPTY_LIST);
    }

    public List<SlideAnnotationNote> getNotes() {
        return notes;
    }

    public Shape getShape() {
        return shape;
    }

    @Override
    public String toString() {
        return "id: " + id + ", creator: " + creator + ", shape: "
                + shapeToString(shape) + ", notes: " + notes;
    }

    final private static class ImmutableShape implements Shape {
        final private Shape shape;

        public boolean contains(double x, double y, double w, double h) {
            return shape.contains(x, y, w, h);
        }

        public boolean contains(double x, double y) {
            return shape.contains(x, y);
        }

        public boolean contains(Point2D p) {
            return shape.contains(p);
        }

        public boolean contains(Rectangle2D r) {
            return shape.contains(r);
        }

        public Rectangle getBounds() {
            return shape.getBounds();
        }

        public Rectangle2D getBounds2D() {
            return shape.getBounds2D();
        }

        public PathIterator getPathIterator(AffineTransform at, double flatness) {
            return shape.getPathIterator(at, flatness);
        }

        public PathIterator getPathIterator(AffineTransform at) {
            return shape.getPathIterator(at);
        }

        public boolean intersects(double x, double y, double w, double h) {
            return shape.intersects(x, y, w, h);
        }

        public boolean intersects(Rectangle2D r) {
            return shape.intersects(r);
        }

        public ImmutableShape(Shape s) {
            // defensive copy
            shape = new Path2D.Double(s);
        }
    }

    public Integer getId() {
        return id;
    }

    static Shape stringToShape(String string) {
        String[] segs = string.split(" ");
        Path2D p = new Path2D.Double();

        int i = 0;
        while (i < segs.length) {
            switch (segs[i++].charAt(0)) {
            case 'Z':
                p.closePath();
                break;
            case 'C':
                p.curveTo(Double.parseDouble(segs[i++]), Double
                        .parseDouble(segs[i++]), Double.parseDouble(segs[i++]),
                        Double.parseDouble(segs[i++]), Double
                                .parseDouble(segs[i++]), Double
                                .parseDouble(segs[i++]));
                break;
            case 'L':
                p.lineTo(Double.parseDouble(segs[i++]), Double
                        .parseDouble(segs[i++]));
                break;
            case 'M':
                p.moveTo(Double.parseDouble(segs[i++]), Double
                        .parseDouble(segs[i++]));
                break;
            case 'Q':
                p.quadTo(Double.parseDouble(segs[i++]), Double
                        .parseDouble(segs[i++]), Double.parseDouble(segs[i++]),
                        Double.parseDouble(segs[i++]));
                break;
            }
        }
        return new ImmutableShape(p);
    }

    static String shapeToString(Shape s) {
        PathIterator p = s.getPathIterator(null);
        StringBuilder sb = new StringBuilder();

        while (!p.isDone()) {
            double coords[] = new double[6];

            switch (p.currentSegment(coords)) {
            case PathIterator.SEG_CLOSE:
                sb.append(" Z");
                break;
            case PathIterator.SEG_CUBICTO:
                sb.append(" C");
                sb.append(" " + coords[0]);
                sb.append(" " + coords[1]);
                sb.append(" " + coords[2]);
                sb.append(" " + coords[3]);
                sb.append(" " + coords[4]);
                sb.append(" " + coords[5]);
                break;
            case PathIterator.SEG_LINETO:
                sb.append(" L");
                sb.append(" " + coords[0]);
                sb.append(" " + coords[1]);
                break;
            case PathIterator.SEG_MOVETO:
                sb.append(" M");
                sb.append(" " + coords[0]);
                sb.append(" " + coords[1]);
                break;
            case PathIterator.SEG_QUADTO:
                sb.append(" Q");
                sb.append(" " + coords[0]);
                sb.append(" " + coords[1]);
                sb.append(" " + coords[2]);
                sb.append(" " + coords[3]);
                sb.append(" " + coords[4]);
                sb.append(" " + coords[5]);
                break;
            }

            p.next();
        }
        return sb.toString().trim();
    }
}
