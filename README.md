# Skilling Cost Calculator

This plugin is intended to calculate the cost/profit/loss for skilling methods. You can pretty easily figure out the initial cost of a skilling method with the skill calculator plugin, but most times you can sell the items back and recoup some of the cost. This provides a convenient way to check skilling methods against current GE prices.

<img width="247" height="991" alt="image" src="https://github.com/user-attachments/assets/21de6f7b-ef18-4c82-8fc5-20fda4a87efb" />

## Current scope

- Much like the skill calculator this plugin can read your current skill level/XP and allows manual input
<img width="235" height="181" alt="image" src="https://github.com/user-attachments/assets/f6ca532e-c3cb-49df-8dcd-1ba0af899757" />

- Automatically attempts to read your current XP when switching skills
- You can choose what level/XP you'd like to get to
- Provides a list of available methods that can be sorted by least action or most profit (least lost in most cases)
<img width="235" height="91" alt="image" src="https://github.com/user-attachments/assets/a8cd2e05-ce16-4883-94df-9c8a6ea99318" />


- Method list is scrollable
- Method list highlights if you have the required skill level for the method
  
<img width="237" height="350" alt="image" src="https://github.com/user-attachments/assets/638fd7f6-f56c-489a-b117-c1537f02bce9" />

- Optional toggle to hide methods you don't meet the requirements for
- Uses live GE pricing to calculate profit/loss
- Toggle to include/exclude GE tax
- Optionally add the cost of extra supplies (like thread for d'hide)
  
<img width="236" height="292" alt="image" src="https://github.com/user-attachments/assets/2018c5fe-1146-4d0c-accd-02f0c549014e" />

- Click the button to run the calculation
<img width="236" height="319" alt="image" src="https://github.com/user-attachments/assets/45abd9cd-ef90-4894-8108-9d33c69e8cf0" />
<img width="227" height="263" alt="image" src="https://github.com/user-attachments/assets/a297a881-9e04-4715-97ba-fa9a2fd23506" />

- Able to look at smithing methods by buying bars or making bars
<img width="243" height="274" alt="image" src="https://github.com/user-attachments/assets/6f874a82-9b93-4311-974f-c1ad934c2677" />

- Prayer method is chosen first (bury, altar, Ectofuntus, bone shards) with a dropdown to choose the bone

<img width="312" height="489" alt="image" src="https://github.com/user-attachments/assets/fe691b6a-65e2-413f-8bd5-2602c18b1b24" />

- Once you select most profitable it will show the prices as well

<img width="388" height="635" alt="image" src="https://github.com/user-attachments/assets/63cd2f0d-eda6-4dc5-8bcc-59d884c97d02" />


- Chaos altar uses the 50% average and buckets of slime are not included since they are untradable

## Notes

The method list might need to be scrolled over to see the full name

## Development
Claude Fable 5 was used to help create this plugin

The icon art was drawn by a wonderful artist named Nikki (I told her to make it a little janky)

This readme was written by me

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the code layout, how method data works (methods are defined in `methods.json`, no code changes needed), calculation assumptions, and how to run locally.
