// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Morphozoic parameters.
 */

#include "parameters.hpp"
#include "../util/fileio.h"
using namespace morphozoic;

// Default parameter values.
const float Parameters::DEFAULT_MIN_RADIUS              = 1.0f;
const float Parameters::DEFAULT_MAX_RADIUS              = 2.5f;
const float Parameters::DEFAULT_DENSITY                 = 0.1f;
const int Parameters::  DEFAULT_CHARGES                 = 2;
const float Parameters::DEFAULT_MAX_CHARGE              = 1.0f;
const float Parameters::DEFAULT_CHARGE_GAUSSIAN_SPREAD  = 1.0f;
const float Parameters::DEFAULT_MAX_INTERACTION_RANGE   = 5.0f;
const float Parameters::DEFAULT_OVERLAP_REPULSION_FORCE = 0.1f;
const float Parameters::DEFAULT_MAX_FORCE               = 20.0f;
const float Parameters::DEFAULT_MAX_SPEED               = 10.0f;
const float Parameters::DEFAULT_UPDATE_STEP             = 0.01f;

// Constructor.
Parameters::Parameters()
{
   MIN_RADIUS              = DEFAULT_MIN_RADIUS;
   MAX_RADIUS              = DEFAULT_MAX_RADIUS;
   DENSITY                 = DEFAULT_DENSITY;
   CHARGES                 = DEFAULT_CHARGES;
   MAX_CHARGE              = DEFAULT_MAX_CHARGE;
   CHARGE_GAUSSIAN_SPREAD  = DEFAULT_CHARGE_GAUSSIAN_SPREAD;
   MAX_INTERACTION_RANGE   = DEFAULT_MAX_INTERACTION_RANGE;
   OVERLAP_REPULSION_FORCE = DEFAULT_OVERLAP_REPULSION_FORCE;
   MAX_FORCE               = DEFAULT_MAX_FORCE;
   MAX_SPEED               = DEFAULT_MAX_SPEED;
   UPDATE_STEP             = DEFAULT_UPDATE_STEP;
}


// Load.
void Parameters::load(FILE *fp)
{
   FREAD_FLOAT(&MIN_RADIUS, fp);
   FREAD_FLOAT(&MAX_RADIUS, fp);
   FREAD_FLOAT(&DENSITY, fp);
   FREAD_INT(&CHARGES, fp);
   FREAD_FLOAT(&MAX_CHARGE, fp);
   FREAD_FLOAT(&CHARGE_GAUSSIAN_SPREAD, fp);
   FREAD_FLOAT(&MAX_INTERACTION_RANGE, fp);
   FREAD_FLOAT(&OVERLAP_REPULSION_FORCE, fp);
   FREAD_FLOAT(&MAX_FORCE, fp);
   FREAD_FLOAT(&MAX_SPEED, fp);
   FREAD_FLOAT(&UPDATE_STEP, fp);
}


// Save.
void Parameters::save(FILE *fp)
{
   FWRITE_FLOAT(&MIN_RADIUS, fp);
   FWRITE_FLOAT(&MAX_RADIUS, fp);
   FWRITE_FLOAT(&DENSITY, fp);
   FWRITE_INT(&CHARGES, fp);
   FWRITE_FLOAT(&MAX_CHARGE, fp);
   FWRITE_FLOAT(&CHARGE_GAUSSIAN_SPREAD, fp);
   FWRITE_FLOAT(&MAX_INTERACTION_RANGE, fp);
   FWRITE_FLOAT(&OVERLAP_REPULSION_FORCE, fp);
   FWRITE_FLOAT(&MAX_FORCE, fp);
   FWRITE_FLOAT(&MAX_SPEED, fp);
   FWRITE_FLOAT(&UPDATE_STEP, fp);
}
