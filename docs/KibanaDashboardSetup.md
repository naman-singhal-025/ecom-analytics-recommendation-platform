# Kibana 7.17.x Dashboard Setup for E-commerce Analytics

This guide is tailored for Kibana and Elasticsearch version **7.17.15**.

## Prerequisites

- Elasticsearch and Kibana 7.17.x are installed and running
- The e-commerce backend application is running and sending data to Elasticsearch
- Access to the Kibana web interface

## Step-by-Step: Quick Setup Using Pre-Built Dashboards

Follow these steps to set up your analytics dashboards in Kibana 7.17.x:

### 1. Open Kibana

- Open Kibana in your web browser (default: http://localhost:5601)

### 2. Import Pre-Built Dashboards

- Go to **Stack Management > Saved Objects**
- Click **Import** (top right) and select the file: `docs/kibana-dashboards-export.ndjson`
- If prompted, choose to overwrite any existing objects to update dashboards.
- After import, you will see dashboards such as "E-commerce Event Overview", "E-commerce Product Analytics", and "E-commerce Category Analytics" in the Dashboard section.

### 3. Create the Index Pattern

- Go to **Stack Management > Index Patterns**
- Click **Create index pattern**
- Enter the following:
  - Index pattern: `user_events*`
- Click **Next step**
- Select the **timestamp** field as the time filter field.
- Click **Create index pattern**

### 4. Open and Use the Dashboards

- Go to **Dashboard** in Kibana's sidebar.
- Open one of the imported dashboards (e.g., "E-commerce Event Overview").
- Set the time range in the top-right (e.g., "Last 15 minutes").
- Click the refresh icon and set an auto-refresh interval (e.g., 10 seconds) for real-time analytics.

### 5. Troubleshooting

- If you don't see any data:
  - Check that the index pattern is set up correctly and selected in the dashboard.
  - Verify that your application is sending data to Elasticsearch.
  - Make sure the time range includes recent data.
  - Ensure field names in the visualizations match your data.

### 6. Next Steps

- Explore the dashboards and visualizations.
- Set up alerts or create custom visualizations as needed.
- See the rest of this document for manual dashboard creation and advanced tips.

---

## Manual Dashboard Creation (Optional)

If you want to create dashboards manually or customize them, follow the steps below.

### 1. Event Overview Dashboard

This dashboard provides an overview of all user events.

1. Navigate to Dashboard > Create dashboard
2. Add the following visualizations:

#### Total Events Counter
- Visualization type: Metric
- Metrics: Count of documents
- Filter: None

#### Events by Type
- Visualization type: Pie chart
- Metrics: Count of documents
- Bucket: Split slices > Terms > Field: eventType

#### Events Over Time
- Visualization type: Line chart
- Metrics: Count of documents
- Bucket: X-axis > Date Histogram > Field: timestamp
- Bucket: Split series > Terms > Field: eventType

#### Top Users
- Visualization type: Data table
- Metrics: Count of documents
- Bucket: Split rows > Terms > Field: userId
- Sort: Metric: Count (descending)
- Size: 10

### 2. Product Analytics Dashboard

This dashboard focuses on product-specific analytics.

1. Navigate to Dashboard > Create dashboard
2. Add the following visualizations:

#### Top Viewed Products
- Visualization type: Horizontal bar chart
- Metrics: Count of documents
- Filter: eventType is "VIEW"
- Bucket: Split bars > Terms > Field: productId
- Sort: Metric: Count (descending)
- Size: 10

#### Top Purchased Products
- Visualization type: Horizontal bar chart
- Metrics: Count of documents
- Filter: eventType is "PURCHASE"
- Bucket: Split bars > Terms > Field: productId
- Sort: Metric: Count (descending)
- Size: 10

#### Product Conversion Rates
- Visualization type: Data table
- Metrics:
  - Count of documents (filter: eventType is "VIEW") > Label: "Views"
  - Count of documents (filter: eventType is "PURCHASE") > Label: "Purchases"
  - Formula: Purchases / Views > Label: "Conversion Rate"
- Bucket: Split rows > Terms > Field: productId
- Sort: Metric: Conversion Rate (descending)
- Size: 10

#### Product Views Over Time
- Visualization type: Line chart
- Metrics: Count of documents
- Filter: eventType is "VIEW"
- Bucket: X-axis > Date Histogram > Field: timestamp
- Bucket: Split series > Terms > Field: productId
- Size: 5

### 3. Category Analytics Dashboard

This dashboard focuses on category-specific analytics.

1. Navigate to Dashboard > Create dashboard
2. Add the following visualizations:

#### Top Categories
- Visualization type: Pie chart
- Metrics: Count of documents
- Bucket: Split slices > Terms > Field: category
- Size: 10

#### Category Trends
- Visualization type: Line chart
- Metrics: Count of documents
- Bucket: X-axis > Date Histogram > Field: timestamp
- Bucket: Split series > Terms > Field: category
- Size: 5

#### Category Conversion Rates
- Visualization type: Data table
- Metrics:
  - Count of documents (filter: eventType is "VIEW") > Label: "Views"
  - Count of documents (filter: eventType is "PURCHASE") > Label: "Purchases"
  - Formula: Purchases / Views > Label: "Conversion Rate"
- Bucket: Split rows > Terms > Field: category
- Sort: Metric: Conversion Rate (descending)
- Size: 10

---

## Setting Up Real-Time Dashboards

For real-time monitoring, you can configure your dashboards to auto-refresh:

1. In the dashboard view, click on the time picker in the top-right corner
2. Set the time range to "Last 15 minutes" or another appropriate range
3. Click on "Refresh" and select an auto-refresh interval (e.g., 10 seconds)

---

## Exporting and Importing Dashboards

You can export your dashboards to share with others or import them into another Kibana instance:

1. Navigate to Stack Management > Saved Objects
2. Select the dashboards and visualizations you want to export
3. Click "Export" and save the .ndjson file
4. To import, click "Import" and select the .ndjson file

---

## Troubleshooting

If you don't see any data in your visualizations:

1. Check that the index pattern is correct
2. Verify that data is being sent to Elasticsearch
3. Check the time range in the dashboard
4. Ensure that the field names in the visualizations match the field names in your data

---

## Next Steps

- Set up alerts based on specific metrics
- Create more advanced visualizations using Vega or TSVB
- Integrate with Canvas for presentation-ready dashboards