program ControlFlowFromFile;
var
  i, sum: integer;
begin
  sum := 0;
  for i := 1 to 5 do
    sum := sum + i;

  while sum < 20 do
    sum := sum + 1;

  if sum = 20 then
    WriteLn('ok=', sum)
  else
    WriteLn('bad');
end.
