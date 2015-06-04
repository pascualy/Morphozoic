/*
 * Programmer : Tom Portegys <portegys@ilstu.edu>
 * File Name : octree.cpp
 *
 * Description : Bounded octree.
 *
 * Date : 3/24/2003
 */

#include "octree.hpp"
#include <assert.h>

// Constructors.
OctObject::OctObject()
{
   Vector point;

   point.Zero();
   init(point, NULL);
}


OctObject::OctObject(float x, float y, float z, void *client)
{
   Vector point;

   point.x = x;
   point.y = y;
   point.z = z;
   init(point, client);
}


OctObject::OctObject(Vector& point, void *client)
{
   init(point, client);
}


void OctObject::init(Vector& point, void *client)
{
   position     = point;
   node         = NULL;
   this->client = client;
}


void OctObject::setClient(void *client)
{
   this->client = client;
}


// Move object.
bool OctObject::move(float x, float y, float z)
{
   Vector point;

   point.x = x;
   point.y = y;
   point.z = z;
   return(move(point));
}


bool OctObject::move(Vector& point)
{
   position = point;
   if (node != NULL)
   {
      return(node->move(this));
   }
   return(false);
}


// Remove object from tree.
void OctObject::remove()
{
   if (node != NULL)
   {
      node->tree->remove(this);
   }
}


// Object is inside tree?
bool OctObject::isInside(Octree *tree)
{
   if (position.x < (tree->center.x - tree->span))
   {
      return(false);
   }
   if (position.x >= (tree->center.x + tree->span))
   {
      return(false);
   }
   if (position.y < (tree->center.y - tree->span))
   {
      return(false);
   }
   if (position.y >= (tree->center.y + tree->span))
   {
      return(false);
   }
   if (position.z < (tree->center.z - tree->span))
   {
      return(false);
   }
   if (position.z >= (tree->center.z + tree->span))
   {
      return(false);
   }
   if (position.x < tree->bounds.xmin)
   {
      return(false);
   }
   if (position.x >= tree->bounds.xmax)
   {
      return(false);
   }
   if (position.y < tree->bounds.ymin)
   {
      return(false);
   }
   if (position.y >= tree->bounds.ymax)
   {
      return(false);
   }
   if (position.z < tree->bounds.zmin)
   {
      return(false);
   }
   if (position.z >= tree->bounds.zmax)
   {
      return(false);
   }
   return(true);
}


// Object is inside node?
bool OctObject::isInside(OctNode *node)
{
   if (position.x < (node->center.x - node->span))
   {
      return(false);
   }
   if (position.x >= (node->center.x + node->span))
   {
      return(false);
   }
   if (position.y < (node->center.y - node->span))
   {
      return(false);
   }
   if (position.y >= (node->center.y + node->span))
   {
      return(false);
   }
   if (position.z < (node->center.z - node->span))
   {
      return(false);
   }
   if (position.z >= (node->center.z + node->span))
   {
      return(false);
   }
   if (node->tree == NULL)
   {
      return(true);
   }
   if (position.x < node->tree->bounds.xmin)
   {
      return(false);
   }
   if (position.x >= node->tree->bounds.xmax)
   {
      return(false);
   }
   if (position.y < node->tree->bounds.ymin)
   {
      return(false);
   }
   if (position.y >= node->tree->bounds.ymax)
   {
      return(false);
   }
   if (position.z < node->tree->bounds.zmin)
   {
      return(false);
   }
   if (position.z >= node->tree->bounds.zmax)
   {
      return(false);
   }
   return(true);
}


// Object is "close"?
bool OctObject::isClose(OctObject *object)
{
#ifdef _DEBUG
   assert(node != NULL);
#endif
   if ((int)(position.SquareDistance(object->position) * node->tree->precision) == 0)
   {
      return(true);
   }
   else
   {
      return(false);
   }
}


// Constructors.
Octree::Octree()
{
   Vector center;

   center.Zero();
   init(center, 1.0f, 0.1f);
}


Octree::Octree(float x, float y, float z, float span, float precision)
{
   Vector center;

   center.x = x;
   center.y = y;
   center.z = z;
   init(center, span, precision);
}


Octree::Octree(Vector& center, float span, float precision)
{
   init(center, span, precision);
}


void Octree::init(Vector& center, float span, float precision)
{
   root            = NULL;
   this->center    = center;
   this->span      = span;
   this->precision = precision;
   bounds.xmin     = center.x - span;
   bounds.xmax     = center.x + span;
   bounds.ymin     = center.y - span;
   bounds.ymax     = center.y + span;
   bounds.zmin     = center.z - span;
   bounds.zmax     = center.z + span;
   objects.clear();
   load = 0;
}


// Destructor.
Octree::~Octree()
{
   clear();
}


void Octree::clear()
{
   if (root != NULL)
   {
      delete root;
   }
   root = NULL;
   objects.clear();
}


// Insert object.
bool Octree::insert(OctObject *object)
{
   bool ret;

#ifdef _DEBUG
   assert(object != NULL);
#endif
   if (!object->isInside(this))
   {
      return(false);
   }

   // Insert into tree.
   if (root == NULL)
   {
      root = new OctNode(center, span, this, NULL, object);
      ret  = true;
   }
   else
   {
      ret = root->insert(object);
   }
   if (ret == false)
   {
      return(false);
   }

   // Insert into object list.
   objects.push_back(object);
   load++;

   return(true);
}


// Remove object.
void Octree::remove(OctObject *object)
{
   register OctObject *o;

   std::list<OctObject *>::iterator itr;

#ifdef _DEBUG
   assert(object != NULL);
#endif
   // Remove from tree.
   if (object->node != NULL)
   {
      object->node->remove(object);
   }

   // Remove from object list.
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      o = *itr;
      if (o == object)
      {
         break;
      }
   }
#ifdef _DEBUG
   assert(itr != objects.end());
#endif
   objects.erase(itr);
   load--;
}


// Search.
// Returns list of matching objects.
void Octree::search(float x, float y, float z, float radius,
                    std::list<OctObject *>& searchList)
{
   Vector point;

   point.x = x;
   point.y = y;
   point.z = z;
   search(point, radius, searchList);
}


void Octree::search(Vector& point, float radius,
                    std::list<OctObject *>& searchList)
{
   searchList.clear();
   if (root != NULL)
   {
      root->search(point, radius, searchList);
   }
}


// Search for visible objects.
// Returns list of matching objects.
void Octree::searchVisible(Frustum                 *frustum,
                           std::list<OctObject *>& searchList)
{
   searchList.clear();
   if (root != NULL)
   {
      root->searchVisible(frustum, searchList);
   }
}


// Set bounds.
void Octree::setBounds(BOUNDS& bounds)
{
   this->bounds = bounds;
}


// Cull objects outside of bounds.
// Returns list of culled objects.
void Octree::cull(std::list<OctObject *>& cullList)
{
   register OctObject *object;

   std::list<OctObject *>           tmpList;
   std::list<OctObject *>::iterator itr;

   cullList.clear();
   tmpList.clear();
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      object = *itr;
      if (!object->isInside(this))
      {
         load--;
         if (object->node != NULL)
         {
            object->node->remove(object);
         }
         cullList.push_back(object);
      }
      else
      {
         tmpList.push_back(object);
      }
   }
   objects.clear();
   for (itr = tmpList.begin(); itr != tmpList.end(); itr++)
   {
      object = *itr;
      objects.push_back(object);
   }
}


// Find median point of objects.
void Octree::findMedian()
{
   register OctObject *o, *o2;

   std::list<OctObject *>::iterator itr;
   float d, m;

   if (load == 0)
   {
      median.x = (bounds.xmax - bounds.xmin) / 2.0f;
      median.y = (bounds.ymax - bounds.ymin) / 2.0f;
      median.z = (bounds.zmax - bounds.zmin) / 2.0f;
      return;
   }

   // Get X median.
   sortObjects(XSORT);
   d = 0.0f;
   m = ((float)load - 1.0f) / 2.0f;
   for (itr = objects.begin(), o2 = NULL;
        itr != objects.end(); itr++, o2 = o)
   {
      o = *itr;
      if (d >= m)
      {
         break;
      }
      d += 1.0f;
   }
   if (d > m)
   {
      median.x = (o2->position.x + o->position.x) / 2.0f;
   }
   else
   {
      median.x = o->position.x;
   }

   // Get Y median.
   sortObjects(YSORT);
   d = 0.0f;
   for (itr = objects.begin(), o2 = NULL;
        itr != objects.end(); itr++, o2 = o)
   {
      o = *itr;
      if (d >= m)
      {
         break;
      }
      d += 1.0f;
   }
   if (d > m)
   {
      median.y = (o2->position.y + o->position.y) / 2.0f;
   }
   else
   {
      median.y = o->position.y;
   }

   // Get Z median.
   sortObjects(ZSORT);
   d = 0.0f;
   for (itr = objects.begin(), o2 = NULL;
        itr != objects.end(); itr++, o2 = o)
   {
      o = *itr;
      if (d >= m)
      {
         break;
      }
      d += 1.0f;
   }
   if (d > m)
   {
      median.z = (o2->position.z + o->position.z) / 2.0f;
   }
   else
   {
      median.z = o->position.z;
   }
}


// Sort objects by dimension.
void Octree::sortObjects(SORTTYPE type)
{
   register OctObject *object, *o;

   std::list<OctObject *>           tmpList;
   std::list<OctObject *>::iterator itr, itr2;

   tmpList.clear();
   while (objects.size() > 0)
   {
      object = NULL;
      for (itr = objects.begin(); itr != objects.end(); itr++)
      {
         o = *itr;
         if (object == NULL)
         {
            object = o;
            itr2   = itr;
         }
         else
         {
            switch (type)
            {
            case XSORT:
               if (o->position.x < object->position.x)
               {
                  object = o;
                  itr2   = itr;
               }
               break;

            case YSORT:
               if (o->position.y < object->position.y)
               {
                  object = o;
                  itr2   = itr;
               }
               break;

            case ZSORT:
               if (o->position.z < object->position.z)
               {
                  object = o;
                  itr2   = itr;
               }
               break;
            }
         }
      }
      objects.erase(itr2);
      tmpList.push_back(object);
   }
   objects.clear();
   for (itr = tmpList.begin(); itr != tmpList.end(); itr++)
   {
      object = *itr;
      objects.push_back(object);
   }
}


#ifdef _DEBUG
// Audit.
void Octree::audit()
{
   register int       count;
   register OctObject *object, *object2;

   std::list<OctObject *>::iterator itr, itr2;

   for (itr = objects.begin(), count = 0; itr != objects.end(); itr++)
   {
      object = *itr;
      assert(object->node != NULL);
      assert(object->node->tree != NULL);
      assert(object->node->tree == this);
      assert(root != NULL && root->findNode(object->node));
      for (itr2 = object->node->objects.begin();
           itr2 != object->node->objects.end(); itr2++)
      {
         object2 = *itr2;
         if (object2 == object)
         {
            break;
         }
      }
#ifdef _DEBUG
      assert(itr2 != object->node->objects.end());
#endif
      count++;
   }
   assert(count == load);
   if (root != NULL)
   {
      assert(root->auditNode(this));
   }
}


bool OctNode::findNode(OctNode *node)
{
   register int i;

   if (node == this)
   {
      return(true);
   }

   for (i = 0; i < 8; i++)
   {
      if ((children[i] != NULL) && children[i]->findNode(node))
      {
         return(true);
      }
   }
   return(false);
}


bool OctNode::auditNode(Octree *tree)
{
   register int       i;
   register OctObject *object, *object2;

   std::list<OctObject *>::iterator itr, itr2;

   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      object = *itr;
      assert(object->node == this);
      assert(object->isInside(this));
      for (itr2 = tree->objects.begin();
           itr2 != tree->objects.end(); itr2++)
      {
         object2 = *itr2;
         if (object2 == object)
         {
            break;
         }
      }
      if (itr2 == tree->objects.end())
      {
         return(false);
      }
   }

   for (i = 0; i < 8; i++)
   {
      if ((children[i] != NULL) && !children[i]->auditNode(tree))
      {
         return(false);
      }
   }
   return(true);
}


#endif

// Constructors.
OctNode::OctNode(float x, float y, float z, float span,
                 Octree *tree, OctNode *parent, OctObject *object)
{
   Vector center;

   center.x = x;
   center.y = y;
   center.z = z;
   init(center, span, tree, parent, object);
}


OctNode::OctNode(Vector& center, float span, Octree *tree,
                 OctNode *parent, OctObject *object)
{
   init(center, span, tree, parent, object);
}


void OctNode::init(Vector& center, float span, Octree *tree,
                   OctNode *parent, OctObject *object)
{
   int i;

   this->center = center;
   this->span   = span;
   this->tree   = tree;
   this->parent = parent;
   for (i = 0; i < 8; i++)
   {
      children[i] = NULL;
   }
   numChildren = 0;
   if (object != NULL)
   {
      objects.push_back(object);
      object->node = this;
   }
}


// Destructor.
OctNode::~OctNode()
{
   register int       i;
   register OctObject *object;

   std::list<OctObject *>::iterator itr;

   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      object = *itr;
      delete object;
   }
   objects.clear();

   for (i = 0; i < 8; i++)
   {
      if (children[i] == NULL)
      {
         continue;
      }
      delete children[i];
      children[i] = NULL;
   }
}


// Insert object.
bool OctNode::insert(OctObject *object, bool upFlag)
{
   float     span2;
   OctObject *o;

   std::list<OctObject *>           tmpList;
   std::list<OctObject *>::iterator itr;

#ifdef _DEBUG
   assert(object != NULL);
#endif
   // Object should be passed up?
   if (upFlag && !object->isInside(this))
   {
      // Try parent.
      if (parent != NULL)
      {
         return(parent->insert(object, upFlag));
      }
      return(false);
   }

   // Insert into this node?
   if (((objects.size() == 0) && (numChildren == 0)) ||
       ((objects.size() > 0) && (*objects.begin())->isClose(object)))
   {
      object->node = this;
      objects.push_back(object);
      return(true);
   }

   // Insert object into child.
   span2 = span / 2.0f;
   if (object->position.z < center.z)
   {
      if (object->position.x < center.x)
      {
         if (object->position.y < center.y)
         {
            if (children[0] == NULL)
            {
               children[0] = new OctNode(center.x - span2, center.y - span2,
                                         center.z - span2, span2, tree, this, object);
               assert(children[0] != NULL);
               numChildren++;
            }
            else
            {
               children[0]->insert(object);
            }
         }
         else
         {
            if (children[1] == NULL)
            {
               children[1] = new OctNode(center.x - span2, center.y + span2,
                                         center.z - span2, span2, tree, this, object);
               assert(children[1] != NULL);
               numChildren++;
            }
            else
            {
               children[1]->insert(object);
            }
         }
      }
      else
      {
         if (object->position.y < center.y)
         {
            if (children[2] == NULL)
            {
               children[2] = new OctNode(center.x + span2, center.y - span2,
                                         center.z - span2, span2, tree, this, object);
               assert(children[2] != NULL);
               numChildren++;
            }
            else
            {
               children[2]->insert(object);
            }
         }
         else
         {
            if (children[3] == NULL)
            {
               children[3] = new OctNode(center.x + span2, center.y + span2,
                                         center.z - span2, span2, tree, this, object);
               assert(children[3] != NULL);
               numChildren++;
            }
            else
            {
               children[3]->insert(object);
            }
         }
      }
   }
   else
   {
      if (object->position.x < center.x)
      {
         if (object->position.y < center.y)
         {
            if (children[4] == NULL)
            {
               children[4] = new OctNode(center.x - span2, center.y - span2,
                                         center.z + span2, span2, tree, this, object);
               assert(children[4] != NULL);
               numChildren++;
            }
            else
            {
               children[4]->insert(object);
            }
         }
         else
         {
            if (children[5] == NULL)
            {
               children[5] = new OctNode(center.x - span2, center.y + span2,
                                         center.z + span2, span2, tree, this, object);
               assert(children[5] != NULL);
               numChildren++;
            }
            else
            {
               children[5]->insert(object);
            }
         }
      }
      else
      {
         if (object->position.y < center.y)
         {
            if (children[6] == NULL)
            {
               children[6] = new OctNode(center.x + span2, center.y - span2,
                                         center.z + span2, span2, tree, this, object);
               assert(children[6] != NULL);
               numChildren++;
            }
            else
            {
               children[6]->insert(object);
            }
         }
         else
         {
            if (children[7] == NULL)
            {
               children[7] = new OctNode(center.x + span2, center.y + span2,
                                         center.z + span2, span2, tree, this, object);
               assert(children[7] != NULL);
               numChildren++;
            }
            else
            {
               children[7]->insert(object);
            }
         }
      }
   }

   // Re-insert existing objects.
   tmpList.clear();
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      o = *itr;
      tmpList.push_back(o);
   }
   objects.clear();
   for (itr = tmpList.begin(); itr != tmpList.end(); itr++)
   {
      o = *itr;
      insert(o);
   }
   return(true);
}


// Remove object.
void OctNode::remove(OctObject *object)
{
   register OctObject *o;

   std::list<OctObject *>::iterator itr;

#ifdef _DEBUG
   assert(object != NULL);
#endif
   // Unlink from object list.
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      o = *itr;
      if (o == object)
      {
         break;
      }
   }
#ifdef _DEBUG
   assert(itr != objects.end());
#endif
   objects.erase(itr);
   o->node = NULL;

   // Contract parent.
   if (parent != NULL)
   {
      parent->contract();
   }
}


// Contract node.
void OctNode::contract()
{
   register int       i, j;
   register OctObject *object;

   std::list<OctObject *>::iterator itr;

   // Delete empty children.
   for (i = 0, j = -1; i < 8; i++)
   {
      if (children[i] == NULL)
      {
         continue;
      }
      if (children[i]->numChildren == 0)
      {
         if (children[i]->objects.size() == 0)
         {
            delete children[i];
            children[i] = NULL;
            numChildren--;
         }
         else
         {
            j = i;
         }
      }
   }

   // Contract parent?
   if (numChildren == 0)
   {
      if (parent != NULL)
      {
         parent->contract();
      }
   }
   else if (numChildren == 1)
   {
      // Bring up single child's objects?
      if (j != -1)
      {
         objects.clear();
         for (itr = children[j]->objects.begin();
              itr != children[j]->objects.end(); itr++)
         {
            object       = *itr;
            object->node = this;
            objects.push_back(object);
         }
         children[j]->objects.clear();
         delete children[j];
         children[j] = NULL;
         numChildren--;
         if (parent != NULL)
         {
            parent->contract();
         }
      }
   }
}


// Move object.
bool OctNode::move(OctObject *object)
{
   register OctObject *o;

   std::list<OctObject *>::iterator itr;
   bool ret;

#ifdef _DEBUG
   assert(object != NULL);
#endif
   // Find object.
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      o = *itr;
      if (o == object)
      {
         break;
      }
   }
#ifdef _DEBUG
   assert(itr != objects.end());
#endif

   // Object remains in node?
   if (object->isInside(this))
   {
      return(true);
   }

   // Remove from node.                  .
   objects.erase(itr);
   o->node = NULL;

   // Insert into parent.
   ret = false;
   if ((parent != NULL) && parent->insert(o, true))
   {
      ret = true;
   }

   // Contract parent?
   if (objects.size() == 0)
   {
      if (parent != NULL)
      {
         parent->contract();
      }
   }

   return(ret);
}


// Search.
// Returns list of matching objects.
void OctNode::search(Vector& point, float radius,
                     std::list<OctObject *>& searchList)
{
   register int       i;
   register OctObject *object;

   std::list<OctObject *>::iterator itr;
   register OctNode                 *child;
   float r2;

   // Get squared search radius.
   r2 = radius * radius;

   // Check for objects within search radius.
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      object = *itr;
      if (object->position.SquareDistance(point) <= r2)
      {
         searchList.push_back(object);
      }
   }

   // Search matching children.
   for (i = 0; i < 8; i++)
   {
      if ((child = children[i]) == NULL)
      {
         continue;
      }
      if ((point.x + radius) < (child->center.x - child->span))
      {
         continue;
      }
      if ((point.x - radius) > (child->center.x + child->span))
      {
         continue;
      }
      if ((point.y + radius) < (child->center.y - child->span))
      {
         continue;
      }
      if ((point.y - radius) > (child->center.y + child->span))
      {
         continue;
      }
      if ((point.z + radius) < (child->center.z - child->span))
      {
         continue;
      }
      if ((point.z - radius) > (child->center.z + child->span))
      {
         continue;
      }
      children[i]->search(point, radius, searchList);
   }
}


// Search for visible objects.
// Returns list of matching objects.
void OctNode::searchVisible(Frustum                 *frustum,
                            std::list<OctObject *>& searchList)
{
   register int       i;
   register OctObject *object;

   std::list<OctObject *>::iterator itr;
   register OctNode                 *child;
   float xmin, xmax, ymin, ymax, zmin, zmax;

   // Node intersects frustum?
   xmin = center.x - span;
   if (xmin < tree->bounds.xmin)
   {
      xmin = tree->bounds.xmin;
   }
   xmax = center.x + span;
   if (xmax > tree->bounds.xmax)
   {
      xmax = tree->bounds.xmax;
   }
   ymin = center.y - span;
   if (ymin < tree->bounds.ymin)
   {
      ymin = tree->bounds.ymin;
   }
   ymax = center.y + span;
   if (ymax > tree->bounds.ymax)
   {
      ymax = tree->bounds.ymax;
   }
   zmin = center.z - span;
   if (zmin < tree->bounds.zmin)
   {
      zmin = tree->bounds.zmin;
   }
   zmax = center.z + span;
   if (zmax > tree->bounds.zmax)
   {
      zmax = tree->bounds.zmax;
   }
   if (!frustum->intersects(xmin, xmax, ymin, ymax, zmin, zmax))
   {
      return;
   }

   // Check for objects within frustum.
   for (itr = objects.begin(); itr != objects.end(); itr++)
   {
      object = *itr;
      if (frustum->isInside(object->position))
      {
         searchList.push_back(object);
      }
   }

   // Search children.
   for (i = 0; i < 8; i++)
   {
      if ((child = children[i]) == NULL)
      {
         continue;
      }
      children[i]->searchVisible(frustum, searchList);
   }
}
