/**
 * Copyright (c) 2011, ReXSL.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the ReXSL.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rexsl.maven.checks;

import com.rexsl.maven.Check;
import com.rexsl.maven.Environment;
import com.rexsl.w3c.CssValidator;
import com.rexsl.w3c.Defect;
import com.rexsl.w3c.ValidationResponse;
import com.rexsl.w3c.ValidatorBuilder;
import com.ymock.util.Logger;
import java.io.File;
import java.util.List;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;

/**
 * Validates CSS files.
 *
 * @author Dmitry Bashkin (dmitry.bashkin@rexsl.com)
 * @version $Id: JigsawCssCheck.java 204 2011-10-26 21:15:28Z guard $
 */
public final class JigsawCssCheck implements Check {

    /**
     * Directory with CSS files.
     */
    private static final String CSS_DIR = "src/webapp/css";

    /**
     * Validator.
     */
    private final transient CssValidator validator =
        new ValidatorBuilder().css();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validate(final Environment env) {
        final File dir = new File(env.webdir(), this.CSS_DIR);
        boolean success = true;
        if (dir.exists()) {
            final String[] exts = new String[] {"css"};
            for (File css : FileUtils.listFiles(dir, exts, true)) {
                try {
                    this.one(css);
                } catch (InternalCheckException ex) {
                    Logger.warn(
                        this,
                        "Failed:\n%[exception]s",
                        ex
                    );
                    success = false;
                }
            }
        } else {
            Logger.info(
                this,
                "%s directory is absent, no CSS tests",
                this.CSS_DIR
            );
        }
        return success;
    }

    /**
     * Check one CSS page.
     * @param file Check this particular CSS document
     * @throws InternalCheckException If some failure inside
     */
    private void one(final File file) throws InternalCheckException {
        String page;
        try {
            page = FileUtils.readFileToString(file);
        } catch (java.io.IOException ex) {
            throw new InternalCheckException(ex);
        }
        final ValidationResponse response = this.validator.validate(page);
        if (!response.valid()) {
            Logger.error(
                this,
                "%s contains invalid CSS",
                file
            );
            for (Defect defect : (List<Defect>) ListUtils
                .union(response.errors(), response.warnings())
            ) {
                Logger.error(
                    this,
                    "[%d] %s: %s",
                    defect.line(),
                    defect.message(),
                    defect.source()
                );
            }
            throw new InternalCheckException(
                "CSS validation failed with %d errors and %d warnings",
                response.errors().size(),
                response.warnings().size()
            );
        }
    }

}
