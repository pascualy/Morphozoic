// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Cell.
 * An object with mass, size, oritentation, and charges arrayed on its surface that
 * attract and repel other cells.
 */

#ifndef __CELL__
#define __CELL__

#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include <assert.h>
#include "parameters.hpp"
#include "../util/random.hpp"
#include "../util/vector.hpp"
#include <GL/glut.h>
using namespace std;

namespace morphozoic
{
// Cell.
class Cell
{
public:

   // Parameters.
   Parameters *parameters;

   // Randomizer.
   Random *randomizer;

   // Properties.
   int           id;                              // id
   float         radius;                          // radius
   float         mass;                            // mass
   vector<float> charges[6];                      // charges (+|-)
   Vector        position;                        // position
   Vector        velocity;                        // velocity
   Vector        forces;                          // impinging forces

   // Orientation vectors:
   // UP=(0,1,0), DOWN=(0,-1,0), RIGHT=(1,0,0), LEFT=(-1,0,0), BACK=(0,0,1), FRONT=(0,0,-1)
   enum ORIENTATION { UP=0, DOWN=1, RIGHT=2, LEFT=3, BACK=4, FRONT=5 };
   int    orientationVectors[6];
   Vector orientationToVector(ORIENTATION);
   ORIENTATION vectorToOrientation(Vector&);

   // Constructors.
   Cell();
   Cell(int id, Parameters *parameters, Random *randomizer,
        float radius, vector<float> *charges,
        Vector& position, Vector& velocity);

   // Get and set ID.
   int getID();
   void setID(int id);

   // Get and set parameters.
   Parameters *getParameters();
   void setParameters(Parameters *);

   // Get and set randomizer.
   Random *getRandomizer();
   void setRandomizer(Random *);

   // Get and set radius.
   float getRadius();
   void setRadius(float radius);

   // Get and set charges.
   vector<float> *getCharges();
   void setCharges(vector<float> *charges);

   // Get and set position.
   Vector& getPosition();
   void setPosition(Vector& position);

   // Get and set velocity.
   Vector& getVelocity();
   void setVelocity(Vector& velocity);

   // Get and set orientation vectors.
   int *getOrientationVectors();
   void setOrientationVectors(int *orientationVectors);

   // Update.
   void update(float step);

   // Draw.
   void draw(bool name = false);

   // Highlight.
   void highlight();

   // Load and save.
   void load(FILE *fp);
   void save(FILE *fp);
};
}
#endif
