/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * Copyright (c) 2003 Tom Portegys, All Rights Reserved.
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */

//***************************************************************************//
//* File Name: frustum.cpp                                                  *//
//* Author:    Tom Portegys, portegys@ilstu.edu                             *//
//* Date Made: 07/25/02                                                     *//
//* File Desc: Class implementation representing a camera frustum.          *//
//* Rev. Date:                                                              *//
//* Rev. Desc:                                                              *//
//*                                                                         *//
//***************************************************************************//

#include "frustum.hpp"

// Extracts The Current View Frustum Plane Equations
// Code from: www.markmorley.com/opengl/frustumculling.html
void
Frustum::ExtractFrustum()
{
    float   proj[16];                             // For Grabbing The PROJECTION Matrix
    float   modl[16];                             // For Grabbing The MODELVIEW Matrix
    float   clip[16];                             // Result Of Concatenating PROJECTION and MODELVIEW
    float   t;                                    // Temporary Work Variable

    glGetFloatv( GL_PROJECTION_MATRIX, proj );    // Grab The Current PROJECTION Matrix
    glGetFloatv( GL_MODELVIEW_MATRIX, modl );     // Grab The Current MODELVIEW Matrix

    // Concatenate (Multiply) The Two Matricies
    clip[ 0] = modl[ 0] * proj[ 0] + modl[ 1] * proj[ 4] + modl[ 2] * proj[ 8] + modl[ 3] * proj[12];
    clip[ 1] = modl[ 0] * proj[ 1] + modl[ 1] * proj[ 5] + modl[ 2] * proj[ 9] + modl[ 3] * proj[13];
    clip[ 2] = modl[ 0] * proj[ 2] + modl[ 1] * proj[ 6] + modl[ 2] * proj[10] + modl[ 3] * proj[14];
    clip[ 3] = modl[ 0] * proj[ 3] + modl[ 1] * proj[ 7] + modl[ 2] * proj[11] + modl[ 3] * proj[15];

    clip[ 4] = modl[ 4] * proj[ 0] + modl[ 5] * proj[ 4] + modl[ 6] * proj[ 8] + modl[ 7] * proj[12];
    clip[ 5] = modl[ 4] * proj[ 1] + modl[ 5] * proj[ 5] + modl[ 6] * proj[ 9] + modl[ 7] * proj[13];
    clip[ 6] = modl[ 4] * proj[ 2] + modl[ 5] * proj[ 6] + modl[ 6] * proj[10] + modl[ 7] * proj[14];
    clip[ 7] = modl[ 4] * proj[ 3] + modl[ 5] * proj[ 7] + modl[ 6] * proj[11] + modl[ 7] * proj[15];

    clip[ 8] = modl[ 8] * proj[ 0] + modl[ 9] * proj[ 4] + modl[10] * proj[ 8] + modl[11] * proj[12];
    clip[ 9] = modl[ 8] * proj[ 1] + modl[ 9] * proj[ 5] + modl[10] * proj[ 9] + modl[11] * proj[13];
    clip[10] = modl[ 8] * proj[ 2] + modl[ 9] * proj[ 6] + modl[10] * proj[10] + modl[11] * proj[14];
    clip[11] = modl[ 8] * proj[ 3] + modl[ 9] * proj[ 7] + modl[10] * proj[11] + modl[11] * proj[15];

    clip[12] = modl[12] * proj[ 0] + modl[13] * proj[ 4] + modl[14] * proj[ 8] + modl[15] * proj[12];
    clip[13] = modl[12] * proj[ 1] + modl[13] * proj[ 5] + modl[14] * proj[ 9] + modl[15] * proj[13];
    clip[14] = modl[12] * proj[ 2] + modl[13] * proj[ 6] + modl[14] * proj[10] + modl[15] * proj[14];
    clip[15] = modl[12] * proj[ 3] + modl[13] * proj[ 7] + modl[14] * proj[11] + modl[15] * proj[15];

    // Extract the RIGHT clipping plane
    planes[0].a = clip[ 3] - clip[ 0];
    planes[0].b = clip[ 7] - clip[ 4];
    planes[0].c = clip[11] - clip[ 8];
    planes[0].d = clip[15] - clip[12];

    // Normalize it
    t = (float) sqrt( planes[0].a * planes[0].a + planes[0].b * planes[0].b + planes[0].c * planes[0].c );
    planes[0].a /= t;
    planes[0].b /= t;
    planes[0].c /= t;
    planes[0].d /= t;

    // Extract the LEFT clipping plane
    planes[1].a = clip[ 3] + clip[ 0];
    planes[1].b = clip[ 7] + clip[ 4];
    planes[1].c = clip[11] + clip[ 8];
    planes[1].d = clip[15] + clip[12];

    // Normalize it
    t = (float) sqrt( planes[1].a * planes[1].a + planes[1].b * planes[1].b + planes[1].c * planes[1].c );
    planes[1].a /= t;
    planes[1].b /= t;
    planes[1].c /= t;
    planes[1].d /= t;

    // Extract the BOTTOM clipping plane
    planes[2].a = clip[ 3] + clip[ 1];
    planes[2].b = clip[ 7] + clip[ 5];
    planes[2].c = clip[11] + clip[ 9];
    planes[2].d = clip[15] + clip[13];

    // Normalize it
    t = (float) sqrt( planes[2].a * planes[2].a + planes[2].b * planes[2].b + planes[2].c * planes[2].c );
    planes[2].a /= t;
    planes[2].b /= t;
    planes[2].c /= t;
    planes[2].d /= t;

    // Extract the TOP clipping plane
    planes[3].a = clip[ 3] - clip[ 1];
    planes[3].b = clip[ 7] - clip[ 5];
    planes[3].c = clip[11] - clip[ 9];
    planes[3].d = clip[15] - clip[13];

    // Normalize it
    t = (float) sqrt( planes[3].a * planes[3].a + planes[3].b * planes[3].b + planes[3].c * planes[3].c );
    planes[3].a /= t;
    planes[3].b /= t;
    planes[3].c /= t;
    planes[3].d /= t;

    // Extract the FAR clipping plane
    planes[4].a = clip[ 3] - clip[ 2];
    planes[4].b = clip[ 7] - clip[ 6];
    planes[4].c = clip[11] - clip[10];
    planes[4].d = clip[15] - clip[14];

    // Normalize it
    t = (float) sqrt( planes[4].a * planes[4].a + planes[4].b * planes[4].b + planes[4].c * planes[4].c );
    planes[4].a /= t;
    planes[4].b /= t;
    planes[4].c /= t;
    planes[4].d /= t;

    // Extract the NEAR clipping plane.  This is last on purpose (see pointinfrustum() for reason)
    planes[5].a = clip[ 3] + clip[ 2];
    planes[5].b = clip[ 7] + clip[ 6];
    planes[5].c = clip[11] + clip[10];
    planes[5].d = clip[15] + clip[14];

    // Normalize it
    t = (float) sqrt( planes[5].a * planes[5].a + planes[5].b * planes[5].b + planes[5].c * planes[5].c );
    planes[5].a /= t;
    planes[5].b /= t;
    planes[5].c /= t;
    planes[5].d /= t;
}


// Box intersects frustum?
bool Frustum::intersects(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax)
{
    int i,j;
    float d;
    Vector verticies[8];

    // Create verticies.
    verticies[0].x = xmin;
    verticies[0].y = ymin;
    verticies[0].z = zmin;
    verticies[1].x = xmin;
    verticies[1].y = ymin;
    verticies[1].z = zmax;
    verticies[2].x = xmin;
    verticies[2].y = ymax;
    verticies[2].z = zmin;
    verticies[3].x = xmin;
    verticies[3].y = ymax;
    verticies[3].z = zmax;
    verticies[4].x = xmax;
    verticies[4].y = ymin;
    verticies[4].z = zmin;
    verticies[5].x = xmax;
    verticies[5].y = ymin;
    verticies[5].z = zmax;
    verticies[6].x = xmax;
    verticies[6].y = ymax;
    verticies[6].z = zmin;
    verticies[7].x = xmax;
    verticies[7].y = ymax;
    verticies[7].z = zmax;

    // Check for vertex intersecting frustum.
    for (i = 0; i < 6; i++)
    {
        for (j = 0; j < 8; j++)
        {
            d = planeToPoint(planes[i], verticies[j]);
            if (d > 0.0) break;
        }
        if (j == 8) return(false);
    }
    return(true);
}


// Is point in frustum?
bool Frustum::isInside(Vector vertex)
{
    for (int i = 0; i < 6; i++)
    {
        if (planeToPoint(planes[i], vertex) < 0.0) return(false);
    }
    return(true);
}


// Plane to point distance.
float Frustum::planeToPoint(struct Plane plane, Vector point)
{
    return((plane.a * point.x) + (plane.b * point.y) + (plane.c * point.z) + plane.d);
}
