/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ramslabs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.statefun.sdk.java.TypeName;
import org.apache.flink.statefun.sdk.java.types.SimpleType;
import org.apache.flink.statefun.sdk.java.types.Type;

public class Messages {

  private static final ObjectMapper mapper = new ObjectMapper();

  /* ingress -> user-shopping-cart */
  public static final Type<AddToCart> ADD_TO_CART =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/AddToCart"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, AddToCart.class));

  /* ingress -> user-shopping-cart */
  public static final Type<ClearCart> CLEAR_CART_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/ClearCart"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, ClearCart.class));

  /* ingress -> user-shopping-cart */
  public static final Type<Checkout> CHECKOUT_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/Checkout"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, Checkout.class));

  /* user-shopping-cart -> egress */
  public static final Type<Receipt> RECEIPT_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/Receipt"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, Receipt.class));

  /* ingress -> stock */
  /* user-shopping-cart -> stock */
  public static final Type<RestockItem> RESTOCK_ITEM_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/RestockItem"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, RestockItem.class));
  
  public static final Type<Product> ADD_PRODUCT_TYPE =
	      SimpleType.simpleImmutableTypeFrom(
	          TypeName.typeNameFromString("com.example/AddProduct"),
	          mapper::writeValueAsBytes,
	          bytes -> mapper.readValue(bytes, Product.class));
  
  public static final Type<Product> DELETE_PRODUCT_TYPE =
	      SimpleType.simpleImmutableTypeFrom(
	          TypeName.typeNameFromString("com.example/DeleteProduct"),
	          mapper::writeValueAsBytes,
	          bytes -> mapper.readValue(bytes, Product.class));
  
  
  
  public static final Type<InventoryOperation> SET_PRODUCT_TYPE =
	      SimpleType.simpleImmutableTypeFrom(
	          TypeName.typeNameFromString("com.example/SetproductType"),
	          mapper::writeValueAsBytes,
	          bytes -> mapper.readValue(bytes, InventoryOperation.class));
  
  public static final Type<InventoryOperation> INCREMENT_PRODUCT_TYPE =
	      SimpleType.simpleImmutableTypeFrom(
	          TypeName.typeNameFromString("com.example/IncrementproductType"),
	          mapper::writeValueAsBytes,
	          bytes -> mapper.readValue(bytes, InventoryOperation.class));
  public static final Type<InventoryOperation> DECREMENT_PRODUCT_TYPE =
	      SimpleType.simpleImmutableTypeFrom(
	          TypeName.typeNameFromString("com.example/DecrementproductType"),
	          mapper::writeValueAsBytes,
	          bytes -> mapper.readValue(bytes, InventoryOperation.class));

  public static class ClearCart {
    private final String userId;

    @JsonCreator
    public ClearCart(@JsonProperty("userId") String userId) {
      this.userId = userId;
    }
    /*        Messages.AddToCart add = new Messages.AddToCart("1", "2", 3);
    String addJson = mapper.writeValueAsString(add);
    System.out.println(addJson);

    String json = "{\"userId\":1, \"quantity\":3, \"itemId\":2}";

    Messages.AddToCart addReceived = mapper.readValue(json.getBytes(), Messages.AddToCart.class);
    System.out.println(addReceived);*/
    public String getUserId() {
      return userId;
    }

    @Override
    public String toString() {
      return "ClearCart{" + "userId='" + userId + '\'' + '}';
    }
  }

  public static class Checkout {
    private final String userId;

    @JsonCreator
    public Checkout(@JsonProperty("userId") String userId) {
      this.userId = userId;
    }

    public String getUserId() {
      return userId;
    }

    @Override
    public String toString() {
      return "Checkout{" + "userId='" + userId + '\'' + '}';
    }
  }

  public static class Receipt {

    private final String userId;
    private final String details;

    public Receipt(@JsonProperty("userId") String userId, @JsonProperty("details") String details) {
      this.userId = userId;
      this.details = details;
    }

    public String getUserId() {
      return userId;
    }

    public String getDetails() {
      return details;
    }

    @Override
    public String toString() {
      return "Receipt{" + "userId='" + userId + '\'' + ", details='" + details + '\'' + '}';
    }
  }

  public static class RestockItem {
    private final String itemId;
    private final int quantity;

    @JsonCreator
    public RestockItem(
        @JsonProperty("itemId") String itemId, @JsonProperty("quantity") int quantity) {
      this.itemId = itemId;
      this.quantity = quantity;
    }

    public String getItemId() {
      return itemId;
    }

    public int getQuantity() {
      return quantity;
    }

    @Override
    public String toString() {
      return "RestockItem{" + "itemId='" + itemId + '\'' + ", quantity=" + quantity + '}';
    }
  }
  
  public static class InventoryOperation {
	    private final String productId;
	    private final int quantity;
	    
	    public enum Operation {
	        SETPRODUCT,
	        INCREMENTPRODUCTS,
	        DECREMENTPRODUCTS
	      }
	    private Operation operation;

	    @JsonCreator
	    public InventoryOperation(
	        @JsonProperty("productId") String itemId, @JsonProperty("quantity") int quantity,@JsonProperty("operation") Operation operation) {
	      this.productId = itemId;
	      this.quantity = quantity;
	      this.operation = operation;
	    }

	    public String getProductId() {
	      return productId;
	    }

	    public int getQuantity() {
	      return quantity;
	    }

	    @Override
	    public String toString() {
	      return "InventoryOperation{" + "operation='" + operation + '\''  + "productId='" + productId + '\'' + ", quantity=" + quantity + '}';
	    }
	  }

  public static class AddToCart {
    private final String userId;
    private final String itemId;
    private final int quantity;

    @JsonCreator
    public AddToCart(
        @JsonProperty("userId") String userId,
        @JsonProperty("itemId") String itemId,
        @JsonProperty("quantity") int quantity) {
      this.userId = userId;
      this.itemId = itemId;
      this.quantity = quantity;
    }

    public String getUserId() {
      return userId;
    }

    public String getItemId() {
      return itemId;
    }

    public int getQuantity() {
      return quantity;
    }

    @Override
    public String toString() {
      return "AddToCart{"
          + "userId='"
          + userId
          + '\''
          + ", itemId='"
          + itemId
          + '\''
          + ", quantity="
          + quantity
          + '}';
    }
  }
  
	public static class Product {
		private final double price;

		public double getPrice() {
			return price;
		}

		public String getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		private final String id;
		private final String description;

		@JsonCreator
		public Product(@JsonProperty("price") double price, @JsonProperty("id") String id,
				@JsonProperty("description") String description) {
			this.price = price;
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return "RequestItem{" + "id=" + id +  "price=" + price + "description=" + description + '}';
		}
	}

  // ---------------------------------------------------------------------
  // Internal messages
  // ---------------------------------------------------------------------

  /* user-shopping-cart -> stock */
  public static final Type<RequestItem> REQUEST_ITEM_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/RequestItem"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, RequestItem.class));

  /* stock -> user-shopping-cart */
  public static final Type<ItemAvailability> ITEM_AVAILABILITY_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameFromString("com.example/ItemAvailability"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, ItemAvailability.class));

  public static final Type<EgressRecord> EGRESS_RECORD_JSON_TYPE =
      SimpleType.simpleImmutableTypeFrom(
          TypeName.typeNameOf("io.statefun.playground", "EgressRecord"),
          mapper::writeValueAsBytes,
          bytes -> mapper.readValue(bytes, EgressRecord.class));

  public static class RequestItem {
    private final int quantity;

    @JsonCreator
    public RequestItem(@JsonProperty("quantity") int quantity) {
      this.quantity = quantity;
    }

    public int getQuantity() {
      return quantity;
    }

    @Override
    public String toString() {
      return "RequestItem{" + "quantity=" + quantity + '}';
    }
  }
  
  

  public static class ItemAvailability {

    public enum Status {
      INSTOCK,
      OUTOFSTOCK
    }

    private final Status status;
    private final int quantity;

    @JsonCreator
    public ItemAvailability(
        @JsonProperty("status") Status status, @JsonProperty("quantity") int quantity) {
      this.status = status;
      this.quantity = quantity;
    }

    public Status getStatus() {
      return status;
    }

    public int getQuantity() {
      return quantity;
    }

    @Override
    public String toString() {
      return "ItemAvailability{" + "status=" + status + ", quantity=" + quantity + '}';
    }
  }

  public static class EgressRecord {
    @JsonProperty("topic")
    private String topic;

    @JsonProperty("payload")
    private String payload;

    public EgressRecord() {
      this(null, null);
    }

    public EgressRecord(String topic, String payload) {
      this.topic = topic;
      this.payload = payload;
    }

    public String getTopic() {
      return topic;
    }

    public String getPayload() {
      return payload;
    }
  }
}
