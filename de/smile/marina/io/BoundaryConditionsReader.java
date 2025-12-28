package de.smile.marina.io;

import de.smile.marina.fem.model.hydrodynamic.BoundaryCondition;

/**
 *
 * @author milbradt
 */
public interface BoundaryConditionsReader {
    
    public BoundaryCondition[] readBoundaryConditions(String[] boundary_condition_key_mask) throws Exception;
    
}
