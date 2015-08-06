// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Morphozoic parameters.
 */

#ifndef __PARAMETERS__
#define __PARAMETERS__

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

/*
 * Morphozoic parameters.
 */

namespace morphozoic
{
class Parameters
{
public:

   static const float DEFAULT_MIN_RADIUS;
   float              MIN_RADIUS;
   static const float DEFAULT_MAX_RADIUS;
   float              MAX_RADIUS;
   static const float DEFAULT_DENSITY;
   float              DENSITY;
   static const int   DEFAULT_CHARGES;
   int                CHARGES;
   static const float DEFAULT_MAX_CHARGE;
   float              MAX_CHARGE;
   static const float DEFAULT_CHARGE_GAUSSIAN_SPREAD;
   float              CHARGE_GAUSSIAN_SPREAD;
   static const float DEFAULT_MAX_INTERACTION_RANGE;
   float              MAX_INTERACTION_RANGE;
   static const float DEFAULT_OVERLAP_REPULSION_FORCE;
   float              OVERLAP_REPULSION_FORCE;
   static const float DEFAULT_MAX_FORCE;
   float              MAX_FORCE;
   static const float DEFAULT_MAX_SPEED;
   float              MAX_SPEED;
   static const float DEFAULT_UPDATE_STEP;
   float              UPDATE_STEP;

   // Constructor.
   Parameters();

   // Load and save parameters.
   void load(FILE *fp);
   void save(FILE *fp);
};
}
#endif
