// For conditions of distribution and use, see copyright notice in morphozoic.h

/*
 * Cell.
 */

#include "cell.hpp"
#include "../util/fileio.h"
using namespace morphozoic;

#ifndef M_PI
#define M_PI    3.14159265f
#endif

// Constructors.
Cell::Cell()
{
   id         = -1;
   parameters = NULL;
   randomizer = NULL;
   mass       = radius = 0.0f;
   for (int i = 0; i < 6; i++)
   {
      orientationVectors[i] = i;
   }
}


Cell::Cell(int id, Parameters *parameters, Random *randomizer,
           float radius, vector<float> *charges,
           Vector& position, Vector& velocity)
{
   this->id         = id;
   this->parameters = parameters;
   this->randomizer = randomizer;
   setRadius(radius);
   setCharges(charges);
   this->position = position;
   this->velocity = velocity;
   for (int i = 0; i < 6; i++)
   {
      orientationVectors[i] = i;
   }
}


// Get ID.
int Cell::getID()
{
   return(id);
}


// Set ID.
void Cell::setID(int id)
{
   this->id = id;
}


// Get parameters.
Parameters *Cell::getParameters()
{
   return(parameters);
}


// Set parameters.
void Cell::setParameters(Parameters *parameters)
{
   this->parameters = parameters;
}


// Get randomizer.
Random *Cell::getRandomizer()
{
   return(randomizer);
}


// Set randomizer.
void Cell::setRandomizer(Random *randomizer)
{
   this->randomizer = randomizer;
}


// Get radius.
float Cell::getRadius()
{
   return(radius);
}


// Set radius.
void Cell::setRadius(float radius)
{
   this->radius = radius;
   float volume = (4.0f * M_PI * radius * radius * radius) / 3.0f;
   mass = volume * parameters->DENSITY;
}


// Get charges.
vector<float> *Cell::getCharges()
{
   return(charges);
}


// Set charges.
void Cell::setCharges(vector<float> *charges)
{
   for (int i = 0; i < 6; i++)
   {
      this->charges[i] = charges[i];
   }
}


// Get position.
Vector& Cell::getPosition()
{
   return(position);
}


// Set position.
void Cell::setPosition(Vector& position)
{
   this->position = position;
}


// Get velocity.
Vector& Cell::getVelocity()
{
   return(velocity);
}


// Set velocity.
void Cell::setVelocity(Vector& velocity)
{
   this->velocity = velocity;
}


// Map orientation to vector.
Vector Cell::orientationToVector(ORIENTATION orientation)
{
   switch (orientationVectors[orientation])
   {
   case UP:
      return(Vector(0.0f, 1.0f, 0.0f));

   case DOWN:
      return(Vector(0.0f, -1.0f, 0.0f));

   case RIGHT:
      return(Vector(1.0f, 0.0f, 0.0f));

   case LEFT:
      return(Vector(-1.0f, 0.0f, 0.0f));

   case BACK:
      return(Vector(0.0f, 0.0f, 1.0f));

   case FRONT:
      return(Vector(0.0f, 0.0f, -1.0f));
   }
   return(Vector(0.0f, 0.0f, 0.0f));
}


// Map vector to orientation.
Cell::ORIENTATION Cell::vectorToOrientation(Vector& v1)
{
   for (int i = 0; i < 6; i++)
   {
      Vector v2 = orientationToVector((Cell::ORIENTATION)i);
      if ((v1.x == v2.x) && (v1.y == v2.y) && (v1.z == v2.z))
      {
         return((Cell::ORIENTATION)i);
      }
   }
   return(Cell::UP);
}


// Get orientation vectors.
int *Cell::getOrientationVectors()
{
   return(orientationVectors);
}


// Set orientation vectors.
void Cell::setOrientationVectors(int *orientationVectors)
{
   for (int i = 0; i < 6; i++)
   {
      this->orientationVectors[i] = orientationVectors[i];
   }
}


// Update cell.
void Cell::update(float step)
{
   if (mass > 0.0f)
   {
      velocity += forces / mass;
      if (velocity.Magnitude() > parameters->MAX_SPEED)
      {
         velocity.Normalize(parameters->MAX_SPEED);
      }
      position += velocity * step;
   }
   forces.Zero();
}


// Draw.
void Cell::draw(bool name)
{
   int     i, j, k;
   float   r;
   Vector  v;
   GLfloat black[4] = { 0.0f, 0.0f, 0.0f, 1.0f };

   // Draw body.
   glDisable(GL_LIGHTING);
   glPushMatrix();
   if (name)
   {
      glPushName(id);
   }
   glTranslatef(position.x, position.y, position.z);
   glCullFace(GL_FRONT);
   glColor3f(0.8f, 0.8f, 0.8f);
   glutSolidSphere(radius, 20, 20);
   glCullFace(GL_BACK);
   if (name)
   {
      glPopName();
   }
   glPopMatrix();

   // Draw orientation axes.
   glPushMatrix();
   glTranslatef(position.x, position.y, position.z);
   Vector up = orientationToVector(UP);
   if (up.z == 1.0f)
   {
      up = Vector(radius * 0.4f, radius * 0.4f, 0.0f);
   }
   else if (up.z == -1.0f)
   {
      up = Vector(-radius * 0.4f, -radius * 0.4f, 0.0f);
   }
   else
   {
      up *= radius * 0.5f;
   }
   glColor3f(1.0f, 0.0f, 0.0f);
   glBegin(GL_LINES);
   glVertex3f(0.0f, 0.0f, 0.0f);
   glVertex3f(up.x, up.y, up.z);
   glEnd();
   glPushMatrix();
   glTranslatef(up.x, up.y, up.z);
   glScalef(.0025, .0025, .0025);
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '1');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glPopMatrix();
   Vector right = orientationToVector(RIGHT);
   if (right.z == 1.0f)
   {
      right = Vector(radius * 0.4f, radius * 0.4f, 0.0f);
   }
   else if (right.z == -1.0f)
   {
      right = Vector(-radius * 0.4f, -radius * 0.4f, 0.0f);
   }
   else
   {
      right *= radius * 0.5f;
   }
   glColor3f(0.0f, 1.0f, 0.0f);
   glBegin(GL_LINES);
   glVertex3f(0.0f, 0.0f, 0.0f);
   glVertex3f(right.x, right.y, right.z);
   glEnd();
   glPushMatrix();
   glTranslatef(right.x, right.y, right.z);
   glScalef(.0025, .0025, .0025);
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '1');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glPopMatrix();
   Vector back = orientationToVector(BACK);
   if (back.z == 1.0f)
   {
      back = Vector(radius * 0.4f, radius * 0.4f, 0.0f);
   }
   else if (back.z == -1.0f)
   {
      back = Vector(-radius * 0.4f, -radius * 0.4f, 0.0f);
   }
   else
   {
      back *= radius * 0.5f;
   }
   glColor3f(0.0f, 0.0f, 1.0f);
   glBegin(GL_LINES);
   glVertex3f(0.0f, 0.0f, 0.0f);
   glVertex3f(back.x, back.y, back.z);
   glEnd();
   glPushMatrix();
   glTranslatef(back.x, back.y, back.z);
   glScalef(.0025, .0025, .0025);
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '0');
   glutStrokeCharacter(GLUT_STROKE_ROMAN, '1');
   glPopMatrix();
   glPopMatrix();

   // Draw charge value rings.
   r = radius * 0.02f;
   for (i = 0; i < 6; i++)
   {
      for (j = 0, k = (int)charges[i].size(); j < k; j++)
      {
         glPushMatrix();
         v = orientationToVector((ORIENTATION)i) * (radius * 0.9f);
         glTranslatef(position.x + v.x, position.y + v.y, position.z + v.z);
         if (fabs(v.y) > 0.0f)
         {
            glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
         }
         else if (fabs(v.x) > 0.0f)
         {
            glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
         }
         if (charges[i][j] > 0.0f)
         {
            glColor3f(charges[i][j] / parameters->MAX_CHARGE, 0.0f, 0.0f);
         }
         else
         {
            glColor3f(0.0f, -charges[i][j] / parameters->MAX_CHARGE, 0.0f);
         }
         glutSolidTorus(r, (radius * 0.1f) + ((float)j * r * 3.0f), 15, 15);
         glPopMatrix();
      }
   }
   glEnable(GL_LIGHTING);
}


// Highlight.
void Cell::highlight()
{
   GLfloat on[4]  = { 0.1f, 0.1f, 0.1f, 1.0f };
   GLfloat off[4] = { 0.0f, 0.0f, 0.0f, 1.0f };

   glMaterialfv(GL_FRONT, GL_EMISSION, on);
   glLineWidth(2.0f);
   glPushMatrix();
   glTranslatef(position.x, position.y, position.z);
   glutWireSphere(radius * 2.0f, 5, 5);
   glPopMatrix();
   glMaterialfv(GL_FRONT, GL_EMISSION, off);
   glLineWidth(1.0f);
}


// Load.
void Cell::load(FILE *fp)
{
   int   i, j, n;
   float f;

   FREAD_INT(&id, fp);
   FREAD_FLOAT(&mass, fp);
   FREAD_FLOAT(&radius, fp);
   FREAD_INT(&n, fp);
   for (i = 0; i < 6; i++)
   {
      charges[i].clear();
      for (j = 0; j < n; j++)
      {
         FREAD_FLOAT(&f, fp);
         charges[i].push_back(f);
      }
   }
   FREAD_FLOAT(&position.x, fp);
   FREAD_FLOAT(&position.y, fp);
   FREAD_FLOAT(&position.z, fp);
   FREAD_FLOAT(&velocity.x, fp);
   FREAD_FLOAT(&velocity.y, fp);
   FREAD_FLOAT(&velocity.z, fp);
   for (i = 0; i < 6; i++)
   {
      FREAD_INT(&n, fp);
      orientationVectors[i] = n;
   }
}


// Save cell.
void Cell::save(FILE *fp)
{
   int   i, j, n;
   float f;

   FWRITE_INT(&id, fp);
   FWRITE_FLOAT(&mass, fp);
   FWRITE_FLOAT(&radius, fp);
   n = (int)charges[0].size();
   FWRITE_INT(&n, fp);
   for (i = 0; i < 6; i++)
   {
      for (j = 0; j < n; j++)
      {
         f = charges[i][j];
         FWRITE_FLOAT(&f, fp);
      }
   }
   FWRITE_FLOAT(&position.x, fp);
   FWRITE_FLOAT(&position.y, fp);
   FWRITE_FLOAT(&position.z, fp);
   FWRITE_FLOAT(&velocity.x, fp);
   FWRITE_FLOAT(&velocity.y, fp);
   FWRITE_FLOAT(&velocity.z, fp);
   for (i = 0; i < 6; i++)
   {
      n = orientationVectors[i];
      FWRITE_INT(&n, fp);
   }
}
