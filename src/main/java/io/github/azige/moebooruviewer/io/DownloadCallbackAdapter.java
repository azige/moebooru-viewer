/*
 * Copyright (C) 2017 Azige
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.azige.moebooruviewer.io;

import java.io.File;

/**
 *
 * @author Azige
 */
public class DownloadCallbackAdapter implements DownloadCallback{

    @Override
    public void onProgress(double rate){
    }

    @Override
    public void onComplete(File file){
    }

    @Override
    public void onFail(Exception ex){
    }
}
