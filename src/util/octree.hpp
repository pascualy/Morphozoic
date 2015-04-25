/*
 * Programmers : Tom Portegys <portegys@ilstu.edu>
 *		 Kevin Greenan <kmgreen2@ilstu.edu>
 *
 * File Name :	octree.hpp
 *
 * Description : Bounded octree.
 *
 * Date : 3/24/2003
 */

#ifndef __OCTREE_HPP__
#define __OCTREE_HPP__

#include <stdio.h>
#include <stdlib.h>
#include <list>
#include "vector.hpp"
#include "frustum.hpp"
using namespace std;

class OctObject;
class Octree;
class OctNode;

// Object in tree.
class OctObject
{
public:

   // Constructors.
   OctObject();
   OctObject(float x, float y, float z, void *client);
   OctObject(Vector& point, void *client);
   void init(Vector& point, void *client);
   void setClient(void *client);
   OctObject operator=(const OctObject& obj);

   // Move object.
   // Returns false if migrating out of bounds.
   bool move(float x, float y, float z);
   bool move(Vector& point);

   // Remove object from tree.
   void remove();

   // Object is inside tree?
   bool isInside(Octree *tree);

   // Object is inside node?
   bool isInside(OctNode *node);

   // Object is "close"?
   bool isClose(OctObject *object);

   // Data members.
   Vector  position;
   OctNode *node;
   void    *client;
};

// Octree.
class Octree
{
public:

   // Bounds.
   typedef struct
   {
      float xmin, xmax;
      float ymin, ymax;
      float zmin, zmax;
   } BOUNDS;

   // Constructors.
   Octree();
   Octree(float x, float y, float z, float span, float precision);
   Octree(Vector& center, float span, float precision);
   void init(Vector& center, float span, float precision);

   // Destructor.
   ~Octree();
   void clear();

   // Insert object.
   bool insert(OctObject *object);

   // Remove object.
   void remove(OctObject *object);

   // Search.
   // Returns list of matching objects.
   void search(float x, float y, float z, float radius,
               list<OctObject *>& searchList);
   void search(Vector& point, float radius,
               list<OctObject *>& searchList);

   // Search for visible objects.
   void searchVisible(Frustum            *frustum,
                      list<OctObject *>& searchList);

   // Set bounds.
   void setBounds(BOUNDS& bounds);

   // Cull objects outside of bounds.
   // Returns list of culled objects.
   void cull(list<OctObject *>& cullList);

   // Find median point of objects.
   void findMedian();

   typedef enum { XSORT, YSORT, ZSORT }
   SORTTYPE;
   void sortObjects(SORTTYPE);

#ifdef _DEBUG
   // Audit.
   void audit();
#endif

   // Data members.
   OctNode           *root;
   Vector            center;
   float             span;
   float             precision;
   BOUNDS            bounds;
   list<OctObject *> objects;
   int               load;
   Vector            median;
};

// Node.
class OctNode
{
public:

   // Constructors.
   OctNode(float x, float y, float z, float span, Octree *tree,
           OctNode *parent, OctObject *object);
   OctNode(Vector& center, float span, Octree *tree,
           OctNode *parent, OctObject *object);
   void init(Vector& center, float span, Octree *tree,
             OctNode *parent, OctObject *object);

   // Destructor.
   ~OctNode();

   // Insert object.
   bool insert(OctObject *object, bool upFlag = false);

   // Remove object.
   void remove(OctObject *object);

   // Contract node.
   void contract();

   // Move object.
   // Returns false if migrating out of bounds.
   bool move(OctObject *object);

   // Search.
   // Returns list of matching objects.
   void search(Vector& point, float radius,
               list<OctObject *>& searchList);

   // Search for visible objects.
   // Returns list of matching objects.
   void searchVisible(Frustum            *frustum,
                      list<OctObject *>& searchList);

#ifdef _DEBUG
   bool auditNode(Octree *);
   bool findNode(OctNode *);
#endif

   // Data members.
   Octree            *tree;
   list<OctObject *> objects;
   OctNode           *parent;
   OctNode           *children[8];
   int               numChildren;
   Vector            center;
   float             span;
};
#endif
