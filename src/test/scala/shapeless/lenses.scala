/*
 * Copyright (c) 2012 Miles Sabin 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shapeless

import org.junit.Test
import org.junit.Assert._

class LensTests {
  import Lens._
  import Nat._
  
  def typed[T](t : => T) {}

  case class Address(street : String, city : String, postcode : String)
  case class Person(name : String, age : Int, address : Address)
  
  implicit val addressIso = HListIso(Address.apply _, Address.unapply _)
  implicit val personIso = HListIso(Person.apply _, Person.unapply _)

  val address = Address("Southover Street", "Brighton", "BN2 9UA")
  val person = Person("Joe Grey", 37, address)
    
  val nameLens     = Lens[Person] >> _0
  val ageLens      = Lens[Person] >> _1
  val addressLens  = Lens[Person] >> _2
  val streetLens   = Lens[Person] >> _2 >> _0
  val cityLens     = Lens[Person] >> _2 >> _1
  val postcodeLens = Lens[Person] >> _2 >> _2

  @Test
  def testBasics {
    val age1 = ageLens.get(person)
    typed[Int](age1)
    assertEquals(37, age1)
    
    val person2 = ageLens.set(person)(38)
    assertEquals(38, person2.age)
    
    val street1 = streetLens.get(person)
    typed[String](street1)
    assertEquals("Southover Street", street1)
    
    val person3 = streetLens.set(person)("Montpelier Road")
    assertEquals("Montpelier Road", person3.address.street)
  }
  
  @Test
  def testCompose {
    val addressLens = Lens[Person] >> _2
    val streetLens = Lens[Address] >> _0
    
    val personStreetLens1 = streetLens compose addressLens
    val personStreetLens2 = compose(streetLens, addressLens)
    val personStreetLens3 = (streetLens :: addressLens :: HNil).reduceLeft(compose)
    
    val street1 = personStreetLens1.get(person)
    typed[String](street1)
    assertEquals("Southover Street", street1)
    
    val street2 = personStreetLens1.get(person)
    typed[String](street2)
    assertEquals("Southover Street", street2)

    val street3 = personStreetLens1.get(person)
    typed[String](street3)
    assertEquals("Southover Street", street3)
  }
  
  @Test
  def testTuples {
    type ISDB = (Int, (String, (Double, Boolean)))
    
    val tp = (23, ("foo", (2.0, false)))

    val lens0 = Lens[ISDB] >> _0
    val lens1 = Lens[ISDB] >> _1
    val lens10 = Lens[ISDB] >> _1 >> _0
    val lens11 = Lens[ISDB] >> _1 >> _1
    val lens110 = Lens[ISDB] >> _1 >> _1 >> _0
    val lens111 = Lens[ISDB] >> _1 >> _1 >> _1
    
    val i = lens0.get(tp)
    typed[Int](i)
    assertEquals(23, i)
    
    val tpi = lens0.set(tp)(13)
    typed[ISDB](tpi)
    assertEquals((13, ("foo", (2.0, false))), tpi)

    val sdb  = lens1.get(tp)
    typed[(String, (Double, Boolean))](sdb)
    assertEquals(("foo", (2.0, false)), sdb)

    val tpsdb = lens1.set(tp)("bar", (3.0, true))
    typed[ISDB](tpsdb)
    assertEquals((23, ("bar", (3.0, true))), tpsdb)

    val s = lens10.get(tp)
    typed[String](s)
    assertEquals("foo", s)

    val tps = lens10.set(tp)("bar")
    typed[ISDB](tps)
    assertEquals((23, ("bar", (2.0, false))), tps)

    val db  = lens11.get(tp)
    typed[(Double, Boolean)](db)
    assertEquals((2.0, false), db)

    val tpdb = lens11.set(tp)(3.0, true)
    typed[ISDB](tpdb)
    assertEquals((23, ("foo", (3.0, true))), tpdb)

    val d = lens110.get(tp)
    typed[Double](d)
    (2.0, d,  Double.MinPositiveValue)

    val tpd = lens110.set(tp)(3.0)
    typed[ISDB](tpd)
    assertEquals((23, ("foo", (3.0, false))), tpd)

    val b = lens111.get(tp)
    typed[Boolean](b)
    assertEquals(false, b)

    val tpb = lens111.set(tp)(true)
    typed[ISDB](tpb)
    assertEquals((23, ("foo", (2.0, true))), tpb)
  }
  
  @Test
  def testHLists {
    type ISB = Int :: String :: Boolean :: HNil 
    val l = 23 :: "foo" :: true :: HNil
    
    val lens0 = hlistNthLens[ISB, _0] 
    val lens1 = hlistNthLens[ISB, _1] 
    val lens2 = hlistNthLens[ISB, _2] 

    val i = lens0.get(l)
    typed[Int](i)
    assertEquals(23, i)
    
    val li = lens0.set(l)(13)
    typed[ISB](li)
    assertEquals(13 :: "foo" :: true :: HNil, li)
    
    val s = lens1.get(l)
    typed[String](s)
    assertEquals("foo", s)
    
    val ls = lens1.set(l)("bar")
    typed[ISB](ls)
    assertEquals(23 :: "bar" :: true :: HNil, ls)
    
    val b = lens2.get(l)
    typed[Boolean](b)
    assertEquals(true, b)

    val lb = lens2.set(l)(false)
    typed[ISB](lb)
    assertEquals(23 :: "foo" :: false :: HNil, lb)
  }

  @Test
  def testSets {
    val s = Set("foo", "bar", "baz")
    val lens = setLens[String]("bar")
    
    val b1 = lens.get(s)
    assert(b1)
    
    val s2 = lens.set(s)(false)
    assertEquals(Set("foo", "baz"), s2)

    val b2 = lens.get(s2)
    assert(!b2)

    val s3 = lens.set(s2)(true)
    assertEquals(s, s3)
  }
  
  @Test
  def testMaps {
    val m = Map(23 -> "foo", 13 -> "bar", 11 -> "baz")
    val lens = mapLens[Int, String](13)
    
    val s1 = lens.get(m)
    assertEquals(Option("bar"), s1)
    
    val m2 = lens.set(m)(Option("wibble"))
    assertEquals(Map(23 -> "foo", 13 -> "wibble", 11 -> "baz"), m2)

    val s2 = lens.get(m2)
    assertEquals(Option("wibble"), s2)

    val m3 = lens.set(m)(None)
    assertEquals(Map(23 -> "foo", 11 -> "baz"), m3)

    val s3 = lens.get(m3)
    assertEquals(None, s3)

    val m4 = lens.set(m3)(Option("bar"))
    assertEquals(m, m4)
    
    val s4 = lens.get(m4)
    assertEquals(Option("bar"), s4)
  }
  
  @Test
  def testProducts {
    val nameAgeCityLens = nameLens * ageLens * cityLens
    
    val nac1 = nameAgeCityLens.get(person) 
    typed[(String, Int, String)](nac1)
    assertEquals(("Joe Grey", 37, "Brighton"), nac1)
    
    val person2 = nameAgeCityLens.set(person)("Joe Soap", 27, "London")
    assertEquals(Person("Joe Soap", 27, Address("Southover Street", "London", "BN2 9UA")), person2)
  }
}