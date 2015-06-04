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
//* File Name: frustum.hpp                                                  *//
//* Author:    Tom Portegys, portegys@ilstu.edu                             *//
//* Date Made: 07/25/02                                                     *//
//* File Desc: Class declaration representing a camera frustum.             *//
//* Rev. Date:                                                              *//
//* Rev. Desc:                                                              *//
//*                                                                         *//
//***************************************************************************//

#ifndef __FRUSTUM_HPP__
#define __FRUSTUM_HPP__

#ifdef UNIX
#include <stdio.h>
#include <stdlib.h>
#else
#include <windows.h>
#endif
#include <GL/gl.h>
#include <math.h>
#include "vector.hpp"

class Frustum
{
    public:

        // Box/frustum bounding plane.
        struct Plane
        {
            float a,b,c,d;                        // Parameters.
        };

        // Constructors.
        Frustum()
        {
            ExtractFrustum();
        }
        Frustum(struct Plane *planes)
        {
            register int i;

            for (i = 0; i < 6; i++)
            {
                this->planes[i] = planes[i];
            }
        }

        // Box/frustum bounding planes.
        struct Plane planes[6];

        // In frustum?
        bool intersects(float xmin, float xmax, float ymin, float ymax, float zmin, float zmax);
        bool isInside(Vector vertex);

        // Plane to point distance.
        float planeToPoint(struct Plane, Vector point);

    private:

        // Extract frustum.
        void ExtractFrustum();

};
#endif
