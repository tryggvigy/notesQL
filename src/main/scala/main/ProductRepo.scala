package main

import main.app.Product

class ProductRepo {
  private val Products = List(
    Product("1", "Cheesecake", "Tasty"),
    Product("2", "Healthy Potion", "+50HP"))

  def product(id: String): Option[Product] =
    Products find (_.id == id)

  def products: List[Product] = Products
}
