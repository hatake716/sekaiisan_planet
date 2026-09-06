#!/usr/bin/env python3
import unittest
from image_sources import license_ok
from assemble_catalog import category
class DataPolicyTest(unittest.TestCase):
    def test_commercial_licenses(self):
        for short,code in [('CC BY 4.0','cc-by-4.0'),('CC BY-SA 3.0','cc-by-sa-3.0'),('CC BY 2.1 jp','cc-by-2.1-jp'),('CC0','cc-zero')]:
            with self.subTest(short=short):self.assertTrue(license_ok(short,'',code)[0])
    def test_rejects_noncommercial_and_no_derivatives(self):
        for short,code in [('CC BY-NC 4.0','cc-by-nc-4.0'),('CC BY-ND 4.0','cc-by-nd-4.0'),('CC BY-NC-SA 4.0','cc-by-nc-sa-4.0')]:
            with self.subTest(short=short):self.assertFalse(license_ok(short,'',code)[0])
    def test_rejects_unverified_and_us_only_public_domain(self):
        for short,code in [('GFDL','gfdl'),('Attribution','attribution'),('Fair use','fair-use'),('',''),('Public domain','pd-us-no-notice')]:
            with self.subTest(short=short):self.assertFalse(license_ok(short,'',code)[0])
    def test_nested_unesco_type_and_mixed_criteria(self):
        pages={'en':{'text':'{{Infobox UNESCO World Heritage Site\n| criteria = {{UNESCO WHS type|(iii), (vii)}}(iii), (vii)\n}}','revision':123}}
        kind,source=category(pages,[])
        self.assertEqual(2,kind);self.assertIn('oldid=123',source)
    def test_unknown_classification_is_never_defaulted_to_cultural(self):
        self.assertEqual((None,''),category({'ja':{'text':'本文','revision':1}},[]))
if __name__=='__main__':unittest.main()
