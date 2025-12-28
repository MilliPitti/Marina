/* ----- AGPL ------------------------------------------------------------------
 * Copyright (C) Peter Milbradt, 1996-2022

 * This file is part of Marina.

 * Marina is free software: you can redistribute it and/or modify              
 * it under the terms of the GNU Affero General Public License as               
 * published by the Free Software Foundation version 3.
 * 
 * Marina is distributed in the hope that it will be useful,                  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                
 * GNU Affero General Public License for more details.                          
 *                                                                              
 * You should have received a copy of the GNU Affero General Public License     
 * along with Marina.  If not, see <http://www.gnu.org/licenses/>.             
 *                                                                               
 * contact: milbradt@smileconsult.de                                        
 * smile consult GmbH                                                           
 * Schiffgraben 11                                                                 
 * 30159 Hannover, Germany 
 * 
 */
package de.smile.marina.fem.model.ground;

import java.io.Serializable;

/**
 * The class "Pair" provides properties and methods of objects for ordered
 * pairs. An ordered pair is a domain consisting of two sequently elements. The
 * order of the elements is fundamental. Therefore it can't be changed and no
 * element can be added or removed to the pair.
 *
 * @author Peter Milbradt
 * @param <X>
 * @param <Y>
 */
public final class Pair<X extends Serializable, Y extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    private X first;
    private Y second;

    public Pair() {
        first = null;
        second = null;
    }

    public Pair(X x, Y y) {
        first = x;
        second = y;
    }

    public Pair(Pair<X, Y> pair) {
        if (pair == null) {
            throw new NullPointerException();
        }
        first = pair.first;
        second = pair.second;
    }

    /**
     * Gets the first element of this ordered pair.
     *
     * @return The method returns the first element of this ordered pair.
     */
    public final X getFirst() {
        return first;
    }

    /**
     * Gets the second element of this ordered pair.
     *
     * @return The method returns the second element of this ordered pair.
     */
    public final Y getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Pair<?, ?>)) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) object;
        return (first.equals(pair.first) && second.equals(pair.second));
    }

    /**
     * Returns a string representation of this ordered pair.
     *
     * @return The method returns a string representation of this ordered pair.
     */
    @Override
    public String toString() {
        return ("(" + first + ", " + second + ")");
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash += 29 * hash + 113 * (first == null ? 0 : first.hashCode());
        hash += 29 * hash + 13 * (second == null ? 0 : second.hashCode());
        return hash;
    }
}
